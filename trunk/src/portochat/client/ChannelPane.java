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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Brandon
 */
public class ChannelPane extends JPanel {
    private static final Logger logger = 
            Logger.getLogger(ChannelPane.class.getName());
    private DefaultListModel participantListModel = new DefaultListModel();
    private JList participantList = new JList(participantListModel);
    private JTextPane viewPane = new JTextPane();
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
                    StyledDocument doc = viewPane.getStyledDocument();
                    try {
                        doc.insertString(doc.getLength(), 
                                myUserName+": ", doc.getStyle("bold"));
                        doc.insertString(doc.getLength(), 
                                textEntry.getText(), doc.getStyle("normal"));
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    sendMessage(textEntry.getText());
                    textEntry.setText("");
                }
            }
        });

        ArrayList<String> me = new ArrayList<String>();
        me.add(myUserName);
        addParticipants(me);
        
        // add text styles
        Style def = StyleContext.getDefaultStyleContext().
                        getStyle(StyleContext.DEFAULT_STYLE);
        StyledDocument doc = viewPane.getStyledDocument();
        Style s = doc.addStyle("normal", def);
        // bold font
        s = doc.addStyle("bold", def);
        StyleConstants.setBold(s, true);
        s = doc.addStyle("joinpart", def);
        StyleConstants.setItalic(s, true);
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
    
    /**
     * Adds a list of participants to this channels list
     * @param participants List of participants in this channel
     */
    public void addParticipants(final List<String> participants) {

        for(String contact : participants) {
            userConnectionEvent(contact, true);
        }
    }
    
    /**
     * Handles connection and disconnection of users in the channel.  If joined
     * is true then the user is added to the channel participant list, otherwise
     * the user is removed from the list.
     * @param user
     * @param joined 
     */
    public void userConnectionEvent(final String user, final boolean joined) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (joined) {
                    if (!participantListModel.contains(user)) {
                        participantListModel.addElement(user);
                        StyledDocument doc = viewPane.getStyledDocument();
                        String message = user + " has joined the channel\n";
                        try {
                            doc.insertString(doc.getLength(), message,
                                    doc.getStyle("joinpart"));
                        } catch (BadLocationException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    participantListModel.removeElement(user);
                    StyledDocument doc = viewPane.getStyledDocument();
                        String message = user + " has left the channel\n";
                        try {
                            doc.insertString(doc.getLength(), message,
                                    doc.getStyle("joinpart"));
                        } catch (BadLocationException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                }
            }
        });
    }

    /**
     * Updates the pane with the received message.  This update is thrown on 
     * the EDT.
     * 
     * @param user
     * @param message 
     */
    public void receiveMessage(final String user, final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StyledDocument doc = viewPane.getStyledDocument();
                    try {
                        doc.insertString(doc.getLength(), 
                                user + ": ", doc.getStyle("bold"));
                        doc.insertString(doc.getLength(), message,
                                doc.getStyle("normal"));
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
            }
        });
    }
    
    /**
     * Updates the list of users in this channel
     * @param list List of users in the channel
     */
    public void updateUserList(final List<String> list) {
        for (String user : list) {
            userConnectionEvent(user, true);
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
