/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

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
     */
    public void sendMessage(String recipient, boolean action, String message);
}
