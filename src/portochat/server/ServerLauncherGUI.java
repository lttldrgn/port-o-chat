/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

/**
 *
 * @author Brandon
 */
public class ServerLauncherGUI extends JFrame implements ActionListener {
    private JButton killButton = new JButton("Kill Server");
    // TODO: Show console with server output
    private JTextArea console = new JTextArea();
    //private PrintStream out = new PrintStream();
    
    public ServerLauncherGUI() {
        add(killButton);
        killButton.addActionListener(this);
        setSize(100,100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private void start() {
        ServerLauncher.launchServer();
    }
    
    public static void main(String args[]) {
       ServerLauncherGUI gui = new ServerLauncherGUI();
       gui.setVisible(true);
       gui.start();
       System.out.println("Server Launched");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(killButton)) {
            // TODO: Exit server more cleanly
            System.out.println("Shutting down server.");
            System.exit(0);
        }
    }
}
