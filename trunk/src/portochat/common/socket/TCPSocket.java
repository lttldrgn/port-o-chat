/*
 *  This file is a part of port-o-chat.
 * 
 *  port-o-chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package portochat.common.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.User;
import portochat.common.Util;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.UserConnection;
import portochat.common.socket.event.NetEvent;
import portochat.common.socket.event.NetListener;
import portochat.common.socket.handler.BufferHandler;
import portochat.common.socket.handler.ChatHandler;
import portochat.common.socket.handler.HandshakeHandler;
import portochat.server.UserDatabase;

/**
 * This class handles server and client TCP connections.
 * 
 * @author Mike
 */
public class TCPSocket {

    private static final Logger logger = Logger.getLogger(TCPSocket.class.getName());
    private String name = "TCPSocket";
    private volatile boolean listening = false;
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private List<NetListener> listeners = null;
    private LinkedBlockingQueue<NetData> writeQueue = null;
    private AcceptThread acceptThread = null;
    private OutgoingThread outgoingThread = null;
    private UserDatabase userDatabase = null;
    private User serverUser = null;
    private boolean encryptedStream = true;
    private volatile boolean isClientSocket = false;

    /*
     * Public constructor
     * 
     * @param name The socket name
     */
    public TCPSocket(String name) {
        this.name = name;
        writeQueue = new LinkedBlockingQueue<NetData>();
        userDatabase = UserDatabase.getInstance();
    }

    /**
     * Binds to the specified port
     * 
     * @param port the port number
     * 
     * @return true if successful
     * @throws IOException
     */
    public boolean bind(int port) throws IOException {

        serverSocket = new ServerSocket(port);
        acceptThread = new AcceptThread();
        acceptThread.start();

        return true;
    }

    /*
     * This method connects to a specified host and port
     * 
     * @param host the hostname
     * @param port the port number
     * 
     * @return true if successful
     */
    public boolean connect(String host, int port) throws IOException {

        serverUser = new User();
        serverUser.setHost(host);

        // TODO cleanup old handlers on reconnect?
        serverUser.addHandler(new HandshakeHandler());
        serverUser.addHandler(new ChatHandler());
        isClientSocket = true;
        clientSocket = new Socket(host, port);
        startProcessingThreads(clientSocket);

        return clientSocket.isConnected();
    }

    /**
     * Calling this method disconnects this socket from the remote host
     */
    public void disconnect() {

        if (outgoingThread != null) {
            outgoingThread.interrupt();
        }
        cleanup();
    }

    /**
     * This method starts the processing threads for the associated socket
     * 
     * @param socket The socket
     */
    private void startProcessingThreads(Socket socket) {

        //TODO: this will create numerous threads.. and for servers how do these close?
        IncomingThread incomingThread = new IncomingThread(socket);
        incomingThread.start();

        if (outgoingThread == null) {
            outgoingThread = new OutgoingThread();
            outgoingThread.start();
        }
    }

    /*
     * This method writes the data to the specified socket
     * 
     * @param socket The socket
     * @param defaultData the data to be sent
     */
    public void writeData(Socket socket, DefaultData defaultData) {
        NetData netData = new NetData();
        netData.socket = socket;
        defaultData.populate();
        netData.data = defaultData.toByteArray();
        writeQueue.offer(netData);
    }

    /**
     * This method is used to add listeners who wish to know about 
     * incoming DefaultData messages
     * 
     * @param listener the NetListener
     */
    public void addListener(NetListener listener) {
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<NetListener>();
        }
        listeners.add(listener);
    }

    /*
     * This method is used to remove listeners from knowing about
     * incoming DefaultData messages
     * 
     * @param listener the NetListener
     */
    public void removeListener(NetListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fires incoming messages
     * 
     * @param socket The socket this message came from
     * @param defulatData the data to be sent
     */
    private void fireIncomingMessage(Socket socket, DefaultData defaultData) {
        NetEvent e = new NetEvent(socket, defaultData);

        if (listeners != null) {

            for (Iterator<NetListener> it = listeners.iterator(); it.hasNext();) {
                NetListener l = it.next();
                l.incomingMessage(e);
            }
        }
    }

    /*
     * @return The client socket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * @return The name of this socket
     */
    public String getName() {
        return name;
    }

    /**
     * Cleans up everything after socket is disconnected
     */
    private synchronized void cleanup() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ex) {
                logger.log(Level.FINE, "Error thrown closing socket", ex);
            } finally {
                clientSocket = null;
            }

        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.INFO, "Error closing server socket", ex);
            }
            listening = false;
        }
        writeQueue.clear();
        // TODO: Should listeners be cleared also?
    }

    private List<BufferHandler> getHandlers(Socket socket) {
        List<BufferHandler> handlers = null;

        User user = (serverUser != null
                ? serverUser : userDatabase.getSocketOfUser(socket));
        handlers = user.getHandlers();

        return handlers;
    }

    private void removeHandler(Socket socket, BufferHandler handler) {
        List<BufferHandler> handlers = getHandlers(socket);

        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /*
     * This class is used to accept incoming connections.
     */
    private class AcceptThread extends Thread {

        public AcceptThread() {
            super("AcceptThread");
        }

        @Override
        public void run() {
            listening = true;

            while (listening) {
                try {
                    Socket socket = serverSocket.accept();
                    userDatabase.addConnection(socket);

                    // Add the handlers
                    userDatabase.clearHandlers(socket);
                    HandshakeHandler handshakeHandler = new HandshakeHandler();
                    handshakeHandler.setServerHandler(true);
                    handshakeHandler.setEncryption(encryptedStream);
                    userDatabase.addHandler(socket, handshakeHandler);
                    ChatHandler chatHandler = new ChatHandler();
                    chatHandler.setServerHandler(true);
                    userDatabase.addHandler(socket, chatHandler);

                    startProcessingThreads(socket);
                } catch (SocketException ex) {
                    logger.log(Level.INFO, "Server Socket closed", ex);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Unable to accept client", ex);
                }
            }
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.INFO, "Error closing server socket", ex);
            }

            logger.log(Level.INFO, "The server has shut down.");
        }
    }

    /**
     * This class is used to processIncoming incoming data.
     */
    private class IncomingThread extends Thread {

        private Socket incomingSocket = null;
        private BufferedInputStream bis = null;
        private User user = null;

        public IncomingThread(Socket incomingSocket) {
            super("IncomingThread");

            this.incomingSocket = incomingSocket;
            user = userDatabase.getSocketOfUser(incomingSocket);
            try {
                bis = new BufferedInputStream(incomingSocket.getInputStream());
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error getting inputstream", ex);
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[8192];

            int length = 0;
            try {
                while ((length = bis.read(buffer)) != -1) {

                    //TODO how to handle chunk data?
                    for (BufferHandler handler : getHandlers(incomingSocket)) {

                        // If finished go to next, remove on outgoing
                        if (handler.isFinished()) {
                            continue;
                        }

                        handler.processIncoming(incomingSocket, buffer, length);

                        // Retrieve any data that needs to be sent to listeners
                        List<DefaultData> listenerDataList = handler.getListenerData();
                        if (listenerDataList != null
                                && listenerDataList.size() > 0) {
                            for (DefaultData defaultData : listenerDataList) {
                                fireIncomingMessage(incomingSocket, defaultData);
                            }
                        }

                        // Retrieve any data that needs to be sent to the socket
                        List<DefaultData> socketDataList = handler.getSocketData();
                        if (socketDataList != null
                                && socketDataList.size() > 0) {
                            for (DefaultData defaultData : socketDataList) {

                                writeData(incomingSocket, defaultData);
                            }
                        }

                        // If this handler consumes the message, stop iterating
                        if (handler.isMessageConsumed()) {
                            break;
                        }
                    }
                }
            } catch (SocketException ex) {
                reportError(user, Level.INFO,
                        "Socket disconnected", ex);
            } catch (IOException ex) {
                reportError(user, Level.INFO,
                        "IOException when reading", ex);
            }

            try {
                sendUserDisconnect();
                incomingSocket.close();
            } catch (IOException ex) {
                reportError(user, Level.INFO,
                        "Error when closing socket", ex);
            }
        }
        
        /**
         * Sends a disconnect message for the user associated to this thread
         * 
         */
        private void sendUserDisconnect() {
            UserConnection userConnection = new UserConnection();
            if (serverSocket != null) {
                // Get the user who disconnected
                if (user == null) {
                    // Hasn't set a username yet
                    user = new User();
                    user.setName("unknown");
                    user.setHost(incomingSocket.getInetAddress().getHostName());
                }
                userConnection.setUser(user);
            }
            userConnection.setConnected(false);
            userConnection.populate();
            fireIncomingMessage(incomingSocket, userConnection);
        }
    }

    /**
     * This class is used to processIncoming outgoing messages
     */
    private class OutgoingThread extends Thread {

        public OutgoingThread() {
            super("Outgoing Thread");
        }

        @Override
        public void run() {
            User user = null;
            // Write the data
            try {
                boolean processingOutbound = true;
                while (processingOutbound) {
                    NetData netData = writeQueue.take();

                    byte[] data = null;
                    user = userDatabase.getSocketOfUser(netData.socket);
                    for (BufferHandler handler : getHandlers(netData.socket)) {
                        
                        if (handler.isFinished()
                                && (handler.getSocketData() == null
                                || handler.getSocketData().isEmpty())) {
                            // If the hander is finished and has no response,
                            // remove the handler 
                            removeHandler(netData.socket, handler);
                            continue;
                        }

                        data = handler.processOutgoing(
                                netData.socket,
                                netData.data, netData.data.length);

                        if (data != null) {
                            try {
                                BufferedOutputStream bos =
                                        new BufferedOutputStream(netData.socket.getOutputStream());
                                logger.log(Level.FINEST, "{0} is writing:{1}",
                                        new Object[]{handler, Util.byteArrayToHexString(data)});
                                bos.write(data);
                                bos.flush();
                            } catch (IOException ex) {
                                if (isClientSocket) {
                                    processingOutbound = false;
                                    reportError(user, Level.INFO,
                                        "Closing connection to server due to exception", ex);
                                } else {
                                    // just log and let the IncomingThread code
                                    // clean up the user information
                                    reportError(user, Level.INFO,
                                        "Closing connection to client due to exception", ex);
                                }
                                break;
                            }
                        }

                        // If handler is finished, remove it
                        if (handler.isFinished()) {
                            removeHandler(netData.socket, handler);
                        }

                        // If this handler consumes the message, stop iterating
                        if (handler.isMessageConsumed()) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException ex) {
                // Thread was interrupted so exit this thread
                reportError(user, Level.FINE,
                        "Exiting outgoing thread due to interruption", ex);
            } finally {
                cleanup();
            }
        }
    }

    /**
     * Reports socket errors
     * 
     * @param user The user
     * @param level The level
     * @param message The message
     * @param ex The exception
     */
    private void reportError(User user, Level level,
            String message, Exception ex) {
        if (logger.isLoggable(level)) {
            StringBuilder sb = new StringBuilder();

            if (user != null) {
                sb.append(user.toString());
                sb.append("> ");
            }
            sb.append(message);

            logger.log(level, sb.toString(), ex);
        }
    }

    /*
     * Used to bundle up the socket and data byte array
     */
    private class NetData {

        Socket socket = null;
        byte[] data = null;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(socket.getInetAddress());
            sb.append("> ");
            sb.append(Util.byteArrayToHexString(data));
            
            return sb.toString();
        }
    }
}
