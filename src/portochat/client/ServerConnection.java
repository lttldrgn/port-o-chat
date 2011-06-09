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
    
    public ServerConnection() {
        
    }
    
    public boolean connectToServer(String serverAddress) {
        boolean successful = true;
        try {
            socket = new Socket(serverAddress, ClientSettings.DEFAULT_SERVER_PORT);
            outWriter = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
            successful = false;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            successful = false;
        }
        return successful;
    }
    
    public void sendPrivateMessage(String username, String message) {
        
    }
    
    public void joinChannel(String channel) {
        outWriter.println(ProtocolDefinitions.JOIN_CHANNEL + channel);
    }
    
    public void leaveChannel(String channel) {
        outWriter.println(ProtocolDefinitions.LEAVE_CHANNEL + channel);
    }
    
    public ArrayList<String> channelWho(String channel) {
        outWriter.println(ProtocolDefinitions.LEAVE_CHANNEL + channel);
        try {
            reader.readLine();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return new ArrayList<String>();
    }
}
