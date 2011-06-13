/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.util.List;

/**
 *
 * @author Brandon
 */
public interface ServerDataListener {
    /**
     * This method is called when a user list is received from the server
     * @param users 
     */
    public void userListReceived(List<String> users);
}
