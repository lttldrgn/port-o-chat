/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import javax.swing.SwingUtilities;

/**
 *
 * @author Brandon
 */
public class ClientLauncher {
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               Client client = new Client();
               client.init();
               client.setVisible(true);
            } 
        });
    }
}
