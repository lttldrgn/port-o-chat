/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lttldrgn.portochat.client;

/**
 * This interface defines methods to access the server and should be implemented
 * by an object that can forward information through a ServerConnection
 * 
 */
public interface ServerConnectionProvider {
    /**
     * Sends a message to the defined recipient
     * @param recipient Person or channel the message is being sent to
     * @param action True if this is an action message
     * @param message Message being sent
     * @return True if the message was attempted to be sent. False if not connected.
     */
    public boolean sendMessage(String recipient, boolean action, String message);
    
    /**
     * Returns the name that the client is connected as on the server
     * @return Username that is set on the server
     */
    public String getConnectedUsername();
}
