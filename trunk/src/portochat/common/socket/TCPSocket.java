/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    public TCPSocket(String name) {
        this.name = name;
        writeQueue = new LinkedBlockingQueue<NetData>();
        protocolHandler = ProtocolHandler.getInstance();
        userDatabase = UserDatabase.getInstance();
    }

    public boolean bind(int port) throws IOException {

        serverSocket = new ServerSocket(port);
        acceptThread = new AcceptThread();
        acceptThread.start();

        return true;
    }

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

    private void startProcessingThreads(Socket socket) {

        //TODO: this will create numerous threads.. and for servers how do these close?
        IncomingThread incomingThread = new IncomingThread(socket);
        incomingThread.start();

        if (outgoingThread == null) {
            outgoingThread = new OutgoingThread();
            outgoingThread.start();
        }

    }

    public void writeData(Socket socket, DefaultData defaultData) {
        NetData netData = new NetData();
        netData.socket = socket;
        defaultData.populate();
        netData.data = defaultData.toByteArray();
        writeQueue.offer(netData);
    }

    public void addListener(NetListener listener) {
        if (listeners == null) {
            listeners = new LinkedList();
        }
        listeners.add(listener);
    }

    public void removeListener(NetListener listener) {
        listeners.remove(listener);
    }

    private void fireIncomingMessage(Socket socket, DefaultData defaultData) {
        NetEvent e = new NetEvent(socket, defaultData);

        if (listeners != null) {

            for (Iterator it = listeners.iterator(); it.hasNext();) {
                NetListener l = (NetListener) it.next();
                l.incomingMessage(e);
            }
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getName() {
        return name;
    }

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

    private class IncomingThread extends Thread {

        private Socket incomingSocket = null;
        private BufferedInputStream bis = null;

        public IncomingThread(Socket incomingSocket) {
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

    private class OutgoingThread extends Thread {

        public OutgoingThread() {
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

    private class NetData {

        private Socket socket = null;
        byte[] data = null;
    }
}
