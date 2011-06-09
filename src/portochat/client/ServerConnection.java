/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.ProtocolDefinitions;

/**
 * Handles all the client interaction with the server
 * 
 */
public class ServerConnection {
    private static final Logger logger = 
            Logger.getLogger(ServerConnection.class.getName());
    private Socket socket = null;
    private PrintWriter outWriter = null;
    private BufferedReader reader = null;
    private volatile boolean connected = false;
    
    public ServerConnection() {
        
    }
    
    public synchronized boolean connectToServer(String serverAddress) {
        if (connected) {
            return false;
        }
        
        boolean successful = true;
        try {
            socket = new Socket(serverAddress, ClientSettings.DEFAULT_SERVER_PORT);
            outWriter = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            IncomingProcessingThread incomingThread = new IncomingProcessingThread();
            incomingThread.start();
            
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
            successful = false;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            successful = false;
        }
        return successful;
    }
    
    public synchronized void disconnectFromServer() {
        try {
            outWriter.close();
            reader.close();
            socket.close();
            connected = false;
        } catch (IOException ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }
    
    public synchronized void sendPrivateMessage(String username, String message) {
        
    }
    
    public synchronized void joinChannel(String channel) {
        outWriter.println(ProtocolDefinitions.JOIN_CHANNEL + channel);
    }
    
    public synchronized void leaveChannel(String channel) {
        outWriter.println(ProtocolDefinitions.LEAVE_CHANNEL + channel);
    }
    
    public synchronized ArrayList<String> channelWho(String channel) {
        outWriter.println(ProtocolDefinitions.LEAVE_CHANNEL + channel);

        return new ArrayList<String>();
    }
    
    private class IncomingProcessingThread extends Thread {
        public IncomingProcessingThread() {
            super("Incoming processing thread");
        }
        
        public void run() {
            while (connected) {
                try {
                    System.out.println(reader.readLine());
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
