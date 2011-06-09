/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author Brandon
 */
public class ClientLauncher {
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               String name = JOptionPane.showInputDialog("Enter your user name");
               Client client = new Client(name);
               client.init();
               client.setSize(400, 600);
               //client.pack();
               client.setVisible(true);
            } 
        });
    }
}
