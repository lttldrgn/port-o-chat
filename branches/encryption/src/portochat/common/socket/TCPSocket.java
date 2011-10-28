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
                    logger.log(Level.INFO, "Server Socket closed");
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

        public IncomingThread(Socket incomingSocket) {
            super("IncomingThread");

            this.incomingSocket = incomingSocket;
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
                logger.log(Level.INFO, "Socket disconnected", ex);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "IOException while reading", ex);
            }

            try {
                sendUserConnectionUpdate(incomingSocket);
                incomingSocket.close();
            } catch (IOException ex) {
                logger.log(Level.INFO, "Exception closing socket", ex);
            }
        }

        /*
         * Sends a user connection update to the specified socket
         * 
         * @param socket The socket to send the info to
         */
        private void sendUserConnectionUpdate(Socket socket) {
            UserConnection userConnection = new UserConnection();
            if (serverSocket != null) {
                // Get the user who disconnected
                User user = userDatabase.getSocketOfUser(socket);
                if (user == null) {
                    // Hasn't set a username yet
                    user = new User();
                    user.setName("unknown");
                    user.setHost(socket.getInetAddress().getHostName());
                }
                userConnection.setUser(user);
            }
            userConnection.setConnected(false);
            userConnection.populate();
            fireIncomingMessage(socket, userConnection);
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
            //TODO do something when disconnecting everyone?

            // Write the data
            try {
                while (true) {
                    NetData netData = writeQueue.take();

                    byte[] data = null;
                    for (BufferHandler handler : getHandlers(netData.socket)) {
                        //TODO how to fix this properly? handshake handler is sending
                        // the user data since the client doesnt' send a response
                        // for the server's READY response.. 
                        if (handler instanceof HandshakeHandler &&
                                !handler.isServerHandler() &&
                                handler.isFinished()) {
                            removeHandler(netData.socket, handler);
                            continue;
                        }
                        data = handler.processOutgoing(
                                netData.socket,
                                netData.data, netData.data.length);

                        if (data != null) {
                            BufferedOutputStream bos =
                                    new BufferedOutputStream(netData.socket.getOutputStream());
                            System.out.println(handler + " is WRITING:" + Util.byteArrayToHexString(data));
                            bos.write(data);
                            bos.flush();
                        }

                        // If handler is finished, remove
                        if (handler.isFinished()) {
                            removeHandler(netData.socket, handler);
                        }
                        
                        // If this handler consumes the message, stop iterating
                        if (handler.isMessageConsumed()) {
                            break;
                        }
                    }
                }


            } catch (SocketException se) {
                logger.log(Level.INFO,
                        "Closing connection due to SocketException", se);
            } catch (IOException ex) {
                logger.log(Level.WARNING,
                        "Closing connection due to IOException", ex);
            } catch (InterruptedException ie) {
                // Thread was interrupted so exit this thread
                logger.fine("Exiting thread due to interruption");
            } finally {
                cleanup();
            }
        }
    }

    /*
     * Used to bundle up the socket and data byte array
     */
    private class NetData {

        Socket socket = null;
        byte[] data = null;
    }
}