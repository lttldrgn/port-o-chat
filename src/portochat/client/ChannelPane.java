/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author Brandon
 */
public class ChannelPane extends JPanel {
    private DefaultListModel participantListModel = new DefaultListModel();
    private JList participantList = new JList(participantListModel);
    private JTextArea viewPane = new JTextArea();
    private JTextArea textEntry = new JTextArea();
    private String channelName = null;
    private String myUserName = null;
    private ServerConnection serverConnection = null;
    
    private ChannelPane(ServerConnection server, String channel, String myName) {
        serverConnection = server;
        this.channelName = channel;
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

        c.gridx = 1;
        c.weightx = 0.1;
        participantList.setPreferredSize(new Dimension(75, 300));
        add(new JScrollPane(participantList), c);
        
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
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

        ArrayList<String> me = new ArrayList<String>();
        me.add(myUserName);
        addContacts(me);
    }
    
    private void sendMessage(String messageText) {
        serverConnection.sendMessage(channelName, messageText);
    }

    public String getPaneTitle() {
        return channelName;
    }
    
    public static ChannelPane createChannelPane(
            ServerConnection serverConnection,
            String channel, 
            String myName) {
        
        ChannelPane channelPane = new ChannelPane(serverConnection, 
                channel, myName);
        channelPane.init();
        return channelPane;
    }
    
    public void addContacts(ArrayList<String> contacts) {
        for(String contact : contacts) {
            participantListModel.addElement(contact);
        }
    }
    
    // main for visual test purposes only
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setSize(600,400);
        frame.getContentPane().add(createChannelPane(null, "channel", "bob"));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
