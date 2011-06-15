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
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.ProtocolHandler;
import portochat.common.protocol.UserConnection;
import portochat.common.socket.event.NetEvent;
import portochat.common.socket.event.NetListener;
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
    private List listeners = null;
    private LinkedBlockingQueue<NetData> writeQueue = null;
    private AcceptThread acceptThread = null;
    private ProtocolHandler protocolHandler = null;
    private OutgoingThread outgoingThread = null;
    private UserDatabase userDatabase = null;

    /*
     * Public constructor
     * 
     * @param name The socket name
     */
    public TCPSocket(String name) {
        this.name = name;
        writeQueue = new LinkedBlockingQueue<NetData>();
        protocolHandler = ProtocolHandler.getInstance();
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
    public boolean connect(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        startProcessingThreads(clientSocket);

        return clientSocket.isConnected();
    }

    /**
     * Thid method starts the processing threads for the associated socket
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
            listeners = new LinkedList();
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

            for (Iterator it = listeners.iterator(); it.hasNext();) {
                NetListener l = (NetListener) it.next();
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
                    startProcessingThreads(serverSocket.accept());
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Unable to accept client", ex);
                }
            }
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Could not close the server socket", ex);
            }

            logger.log(Level.INFO, "The server has shut down.");
        }
    }

    /**
     * This class is used to process incoming data.
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
                logger.log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[8192];

            while (incomingSocket.isConnected()) {

                int length = 0;
                try {
                    length = bis.read(buffer);
                } catch (SocketException ex) {
                    break;
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }

                //TODO handle over 8192.. is that MTU?
                List<DefaultData> defaultDataList = protocolHandler.processData(buffer, length);
                for (DefaultData defaultData : defaultDataList) {
                    fireIncomingMessage(incomingSocket, defaultData);
                }
            }

            try {
                sendUserConnectionUpdate(incomingSocket);
                incomingSocket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
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
                String user = userDatabase.getSocketOfUser(socket);
                if (user == null) {
                    // Hasn't set a username yet
                    user = socket.getInetAddress().toString();
                }
                userConnection.setUser(user);
            }
            userConnection.setConnected(false);
            userConnection.populate();
            fireIncomingMessage(socket, userConnection);
        }
    }

    /**
     * This class is used to process outgoind messages
     */
    private class OutgoingThread extends Thread {

        public OutgoingThread() {
            super("Outgoing Thread");
        }

        @Override
        public void run() {
            //TODO do something when disconnecting everyone?
            while (true) {

                // Write the data
                try {
                    NetData netData = writeQueue.take();
                    BufferedOutputStream bos =
                            new BufferedOutputStream(netData.socket.getOutputStream());
                    bos.write(netData.data);
                    bos.flush();

                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (InterruptedException ie) {
                    // TODO: interruption probably means stop listening
                    // return;
                }

            }
        }
    }

    /*
     * Used to bundle up the socket and data byte array
     */
    private class NetData {

        private Socket socket = null;
        byte[] data = null;
    }
}
