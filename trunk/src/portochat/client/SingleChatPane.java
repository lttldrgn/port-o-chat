/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 *
 * @author Brandon
 */
public class SingleChatPane extends JPanel implements ActionListener {
    private JTextArea viewPane = new JTextArea();
    private JTextArea textEntry = new JTextArea(1, 80);
    private JButton sendButton = new JButton("Send");
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
        setLayout(new BorderLayout());
        add(viewPane, BorderLayout.CENTER);
        viewPane.setEditable(false);
        JPanel sendPanel = new JPanel();
        sendPanel.setLayout(new BorderLayout());
        sendPanel.add(textEntry, BorderLayout.CENTER);
        sendPanel.add(sendButton, BorderLayout.LINE_END);
        add(sendPanel, BorderLayout.PAGE_END);
        sendButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        viewPane.append(myUserName + ":" + textEntry.getText() + "\n");
        sendMessage(textEntry.getText());
        textEntry.setText("");
    }
    
    private void sendMessage(String messageText) {
        serverConnection.sendMessage(recipient, messageText);
    }

    public String getPaneTitle() {
        return recipient;
    }
    
    public static SingleChatPane createChatPane(
            ServerConnection serverConnection,
            String paneTitle, 
            String myName) {
        
        SingleChatPane chatPane = new SingleChatPane(serverConnection, 
                paneTitle, myName);
        chatPane.init();
        return chatPane;
    }
    
}
