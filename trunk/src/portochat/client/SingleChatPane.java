/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author Brandon
 */
public class SingleChatPane extends JPanel {
    private JTextArea viewPane = new JTextArea();
    private JTextArea textEntry = new JTextArea();
    private String recipient = null;
    private String myUserName = null;
    private ServerConnection serverConnection = null;
    
    private SingleChatPane(ServerConnection server, String recipient, String myName) {
        serverConnection = server;
        this.recipient = recipient;
        this.myUserName = myName;
    }
    
    private void init() {
        // construct GUI
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.9;
        c.weighty = 0.9;
        add(new JScrollPane(viewPane), c);
        viewPane.setEditable(false);
        
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.1;
        add(new JScrollPane(textEntry), c);
 
        textEntry.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    viewPane.append(myUserName + ":" + textEntry.getText());
                    sendMessage(textEntry.getText());
                    textEntry.setText("");
                }
            }
        });

        textEntry.requestFocus();
    }
    
    private void sendMessage(String messageText) {
        serverConnection.sendMessage(recipient, messageText);
    }

    public String getPaneTitle() {
        return recipient;
    }
    
    public static SingleChatPane createChatPane(
            ServerConnection serverConnection,
            String recipient, 
            String myName) {
        
        SingleChatPane chatPane = new SingleChatPane(serverConnection, 
                recipient, myName);
        chatPane.init();
        return chatPane;
    }
    
}