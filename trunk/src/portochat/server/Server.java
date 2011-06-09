/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private ServerSocket serverSocket = null;
    private AcceptThread acceptThread = null;
    private boolean listening = false;
    private final Object lock = new Object();
    
    public Server() {
    }

    public boolean listen(int port) {

        boolean success = true;

        try {
            serverSocket = new ServerSocket(port);
            logger.log(Level.INFO, "Listening on port: {0}", port);
            acceptThread = new AcceptThread();
            acceptThread.start();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't listen on port: " + port, ex);
            success = false;
        }

        return success;
    }

    public void shutdown() {
        listening = false;
    }

    private class AcceptThread extends Thread {

        public AcceptThread() {
            super("AcceptThread");
        }

        @Override
        public void run() {
            synchronized (lock) {
                listening = true;

                while (listening) {
                    try {
                        new ServerThread(serverSocket.accept()).start();
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
    }

    private class ServerThread extends Thread {

        private Socket socket = null;

        public ServerThread(Socket socket) {
            super("ServerThread-" + socket.getInetAddress());
            logger.log(Level.INFO, "Accepting connection from: {0}",
                    socket.getInetAddress());
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(),
                        true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                        socket.getInputStream()));

                String input = null;

                while ((input = in.readLine()) != null) {
                    // Process input
                    System.out.println(socket.getInetAddress() + ": " + input);
                    //out.println(outputLine);
                }
                
                logger.log(Level.INFO, "{0} has disconnected", socket.getInetAddress());
                out.close();
                in.close();
                socket.close();

            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error on server stream.", ex);
            }
        }
    }
}
