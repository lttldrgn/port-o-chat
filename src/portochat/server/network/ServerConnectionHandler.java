/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.network.ConnectionHandler;
import portochat.common.network.handler.ChatHandler;

/**
 * Extends ConnectionHandler with server specific code for network handling
 * @author Brandon
 */
public class ServerConnectionHandler extends ConnectionHandler {
    private final static Logger logger = Logger.getLogger(ServerConnectionHandler.class.getName());
    private ServerSocket serverSocket = null;
    private AcceptThread acceptThread = null;
    private volatile boolean listening = false;
    private final boolean encryptedStream = true;
    
    public ServerConnectionHandler(String name) {
        super(name);
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
    
    @Override
    protected synchronized void cleanup() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.INFO, "Error closing server socket", ex);
            }
            listening = false;
        }
        super.cleanup();
        // TODO: Should listeners be cleared also?
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
    
}
