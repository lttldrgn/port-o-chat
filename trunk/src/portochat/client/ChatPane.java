/*
 *  This file is a part of port-o-chat.
 * 
 *  port-o-chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package portochat.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import portochat.common.Settings;

/**
 *
 * @author Brandon
 */
public class ChatPane extends JPanel {

    private static final Logger logger =
            Logger.getLogger(ChatPane.class.getName());
    private DefaultListModel participantListModel = null;
    private JList participantList = null;
    private JTextPane viewPane = new JTextPane();
    private JTextArea textEntry = new JTextArea();
    private String recipient = null;
    private String myUserName = null;
    private ServerConnection serverConnection = null;
    private boolean isChannel = false;
    private static final SimpleDateFormat formatDate =
            new SimpleDateFormat("h:mm.ssa");

    /**
     * Creates a Chat Pane
     * @param server
     * @param recipient Recipient of messages coming from this chat pane, either
     * a user name or channel name
     * @param myName 
     * @param channel True if this chat pane is a channel
     */
    private ChatPane(ServerConnection server, String recipient, 
            String myName, boolean isChannel) {
        serverConnection = server;
        this.recipient = recipient;
        this.myUserName = myName;
        this.isChannel = isChannel;
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

        if (isChannel) {
            participantListModel = new DefaultListModel();
            participantList = new JList(participantListModel);

            c.gridx = 1;
            c.weightx = 0.1;
            participantList.setPreferredSize(new Dimension(75, 300));
            add(new JScrollPane(participantList), c);
            
            //set up gridwidth for next component
            c.gridwidth = 2;
        }

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.1;
        add(new JScrollPane(textEntry), c);

        textEntry.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    String text = textEntry.getText();
                    if (text.isEmpty() || text.startsWith("\n")) {
                        textEntry.setText("");
                        return;
                    }

                    
                    StyledDocument doc = viewPane.getStyledDocument();
                    boolean action = false;

                    // Check for commands
                    if (text.startsWith(Settings.COMMAND_PREFIX)) {
                        if (text.startsWith(Settings.COMMAND_PREFIX + "me ")) {
                            action = true;
                            text = text.replaceFirst(Settings.COMMAND_PREFIX + "me", "");
                        } else {
                            try {
                                doc.insertString(doc.getLength(),
                                        getTimestamp() + " Unknown command: "
                                        + text.split(" ")[0],
                                        doc.getStyle("unknowncommand"));
                                return;
                            } catch (BadLocationException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                    // Display message
                    try {
                        if (action) {
                            doc.insertString(doc.getLength(),
                                    getTimestamp() + " * " + myUserName + " ",
                                    doc.getStyle("boldaction"));
                            doc.insertString(doc.getLength(),
                                    text,
                                    doc.getStyle("action"));
                        } else {
                            doc.insertString(doc.getLength(),
                                    getTimestamp() + " " + myUserName + ": ",
                                    doc.getStyle("bold"));
                            doc.insertString(doc.getLength(),
                                    text + "\n",
                                    doc.getStyle("normal"));
                        }
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    sendMessage(action, text);
                    textEntry.setText("");
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        textEntry.setText("");
                }
            }
        });

        if (isChannel) {
            ArrayList<String> me = new ArrayList<String>();
            me.add(myUserName);
            addParticipants(me);
        }
        initStyles(viewPane);

    }
    
    private void initStyles(JTextPane viewPane) {
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
        s = doc.addStyle("disconnect", def);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, new Color(0, 153, 0));
        s = doc.addStyle("boldaction", def);
        StyleConstants.setItalic(s, true);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, new Color(145, 25, 139));
        s = doc.addStyle("action", def);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, new Color(145, 25, 139));
        s = doc.addStyle("unknowncommand", def);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, new Color(219, 90, 39));
    }

    private void sendMessage(boolean action, String messageText) {
        serverConnection.sendMessage(recipient, action, messageText);
    }

    public String getPaneTitle() {
        return recipient;
    }

    /**
     * Creates a Chat Pane
     * @param serverConnection
     * @param recipient Recipient of messages coming from this chat pane, either
     * a user name or channel name
     * @param myName 
     * @param channel True if this chat pane is a channel
     */
    public static ChatPane createChatPane(
            ServerConnection serverConnection,
            String channel,
            String myName,
            boolean isChannel) {

        ChatPane channelPane = new ChatPane(serverConnection,
                channel, myName, isChannel);
        channelPane.init();
        return channelPane;
    }

    /**
     * Adds a list of participants to this channels list
     * @param participants List of participants in this channel
     */
    public void addParticipants(final List<String> participants) {

        for (String contact : participants) {
            userJoinedEvent(contact, true);
        }
    }

    /**
     * Handles joining and parting of users in the channel.  If joined
     * is true then the user is added to the channel participant list, otherwise
     * the user is removed from the list.
     * @param user
     * @param joined 
     */
    public void userJoinedEvent(final String user, final boolean joined) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (joined) {
                    if (!participantListModel.contains(user)) {
                        participantListModel.addElement(user);
                        StyledDocument doc = viewPane.getStyledDocument();
                        String message = getTimestamp() + " " + user
                                + " has joined the channel\n";
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
                    String message = getTimestamp() + " " + user
                            + " has left the channel\n";
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
     * Handles users who are disconnecting from the server while in the channel.
     * This will remove the user from the list.
     * @param user
     */
    public void userDisconnectedEvent(final String user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (participantListModel.contains(user)) {
                    participantListModel.removeElement(user);
                    StyledDocument doc = viewPane.getStyledDocument();
                    String message = getTimestamp() + " " + user
                            + " has disconnected from the server\n";
                    try {
                        doc.insertString(doc.getLength(), message,
                                doc.getStyle("disconnect"));
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
     * @param action
     * @param message 
     */
    public void receiveMessage(final String user, final boolean action,
            final String message) {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                StyledDocument doc = viewPane.getStyledDocument();
                try {
                    if (action) {
                        doc.insertString(doc.getLength(),
                                getTimestamp() + " * " + user + " ",
                                doc.getStyle("boldaction"));
                        doc.insertString(doc.getLength(),
                                message,
                                doc.getStyle("action"));
                    } else {
                        doc.insertString(doc.getLength(),
                                getTimestamp() + " " + user + ": ",
                                doc.getStyle("bold"));
                        doc.insertString(doc.getLength(),
                                message + "\n",
                                doc.getStyle("normal"));
                    }
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
            userJoinedEvent(user, true);
        }
    }

    private String getTimestamp() {
        Date currentDate = new Date();
        return formatDate.format(currentDate);
    }

    // main for visual test purposes only
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setSize(600, 400);
        frame.getContentPane().add(createChatPane(null, "channel", "bob", true));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
