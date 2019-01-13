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
package com.lttldrgn.portochat.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.lttldrgn.portochat.common.network.ConnectionHandler;
import com.lttldrgn.portochat.common.network.handler.ChatHandler;

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
