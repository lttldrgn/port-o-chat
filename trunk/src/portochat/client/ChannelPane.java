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
    private static final SimpleDateFormat formatDate =
            new SimpleDateFormat("h:mm.ssa");

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
                    boolean action = false;
                    String text = textEntry.getText();

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
                                    text,
                                    doc.getStyle("normal"));
                        }
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    sendMessage(action, text);
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
        serverConnection.sendMessage(channelName, action, messageText);
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
                                message,
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
        frame.getContentPane().add(createChannelPane(null, "channel", "bob"));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
