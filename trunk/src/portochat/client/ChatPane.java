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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import portochat.common.Settings;
import portochat.common.User;
import portochat.common.Util;

/**
 *
 * @author Brandon
 */
public class ChatPane extends JPanel implements PropertyChangeListener {

    private static final Logger logger =
            Logger.getLogger(ChatPane.class.getName());
    private DefaultListModel participantListModel = null;
    private JList participantList = null;
    private JPopupMenu viewPaneRightClickMenu;
    private JTextPane viewPane = new JTextPane();
    private JTextArea textEntry = new JTextArea();
    private String recipient = null;
    private String myUserName = null;
    private ServerConnectionProvider serverConnectionProvider = null;
    private boolean isChannel = false;
    private Element chatTextElement = null; // element that chat text is inserted
    private HTMLDocument htdoc = null;

    /**
     * Creates a Chat Pane
     * @param serverProvider Provider of the server connection
     * @param recipient Recipient of messages coming from this chat pane, either
     * a user name or channel name
     * @param myName 
     * @param channel True if this chat pane is a channel
     */
    private ChatPane(ServerConnectionProvider serverProvider, String recipient, 
            String myName, boolean isChannel) {
        serverConnectionProvider = serverProvider;
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
        c.weightx = 0.8;
        c.weighty = 0.9;
        JScrollPane viewScroll = new JScrollPane(viewPane);
        viewScroll.setPreferredSize(new Dimension(400, 400));
        viewScroll.setMinimumSize(new Dimension(200, 200));
        add(viewScroll, c);
        viewPane.setEditable(false);

        if (isChannel) {
            participantListModel = new DefaultListModel();
            participantList = new JList(participantListModel);

            c.gridx = 1;
            c.weightx = 0.2;
            JScrollPane participantScroll = new JScrollPane(participantList);
            participantScroll.setPreferredSize(new Dimension(100, 300));
            participantScroll.setMinimumSize(new Dimension(100, 100));
            add(participantScroll, c);
            
            //set up gridwidth for next component
            c.gridwidth = 2;
        }

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.8;
        c.weighty = 0.1;
        JScrollPane textEntryScroll = new JScrollPane(textEntry);
        textEntryScroll.setPreferredSize(new Dimension(500, 50));
        add(textEntryScroll, c);
        
        textEntry.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    String text = textEntry.getText();
                    if (text.isEmpty() || text.startsWith("\n")) {
                        textEntry.setText("");
                    } else {
                        processInputMessage(text);
                    }
                    e.consume();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Added this check here for when MS IME is being used
                    // with some character sets and for some reason we do not 
                    // get the key pressed events, but we do get key released 
                    // events so we can catch the input here
                    String text = textEntry.getText();
                    if (!text.isEmpty() && !text.startsWith("\n")) {
                        processInputMessage(text);
                    }
                    textEntry.setText("");
                }
            }
        });

        if (isChannel) {
            ArrayList<User> me = new ArrayList<User>();
            me.add(new User(myUserName, "localhost"));
            addParticipants(me);
        }
        initStyles(viewPane);
        setupRightClickMenu();
        // disabling for now... not being used at the moment and is a memory 
        // leak anyway because we were not unregistering on pane close
        //ThemeManager.getInstance().addThemeListener(this);
        
        viewPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            try {
                                desktop.browse(URI.create(e.getDescription()));
                                textEntry.requestFocusInWindow();
                                viewPane.setCaretPosition(
                                        viewPane.getStyledDocument().getLength());
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(viewPane, 
                                        "Could not launch default browser. See log for reason.");
                                logger.log(Level.INFO, 
                                        "Could not launch browser.", ex);
                            }
                        }
                    }
                }
            }
            
        });
    }
    
    private void initStyles(JTextPane viewPane) {
        viewPane.setEditorKit(new HTMLEditorKit());
        
        htdoc = (HTMLDocument) viewPane.getStyledDocument();
        resetViewPane();
    }
    
    private void resetViewPane() {
        StringBuilder startContent = new StringBuilder();
        startContent.append("<html><head>");

        // define styles
        startContent.append("<style type=\"text/css\">");
        startContent.append(".joinpart {color:rgb(0, 153, 0); font-style:italic}");
        startContent.append(".bold {font-weight:bold; }");
        startContent.append(".boldaction {color:rgb(145, 25, 139); font-weight:bold; font-style:italic}");
        startContent.append(".action {color:rgb(145, 25, 139); font-style:italic}");
        startContent.append(".disconnect {color:rgb(0, 0, 153); font-style:italic}");
        startContent.append(".unknowncommand {color:rgb(219, 90, 39); font-style:italic}");
        startContent.append("</style>");

        startContent.append("</head><body id=\"body\">");
        startContent.append("<p id=\"chatText\"></p>");
        startContent.append("</body></html>");
        
        viewPane.setText(startContent.toString());
        chatTextElement = htdoc.getElement("chatText");
        
    }
    
    private void setupRightClickMenu() {
        viewPaneRightClickMenu = new JPopupMenu();

        JMenuItem clear = new JMenuItem("Clear");
        viewPaneRightClickMenu.add(clear);
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetViewPane();
            }
        });

        viewPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    viewPaneRightClickMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
    }
    
    /**
     * Converts any text starting with 'http://' or 'www' to HTML anchor links
     * @param text
     * @return Text with any links converted to HTML anchor tags
     */
    private String convertLinks(String text) {
        String returnText = text;
        String temp = text.toLowerCase();
        if (temp.contains("http://") || text.contains("www")) {
            int currentIndex = getNextLinkIndex(text, 0);
            
            StringBuilder sb = new StringBuilder(text);
            while (currentIndex != -1 && currentIndex < temp.length()) {
                int endLink = temp.indexOf(" ", currentIndex);
                if (endLink == -1) {
                    endLink = temp.length();
                }
                String link = temp.substring(currentIndex, endLink);
                try {
                    // check validity of link
                    URI uri = new URI(link);
                } catch (URISyntaxException ex) {
                    // bad link so do not modify anything
                    currentIndex = endLink;
                    continue;
                }
                sb.insert(currentIndex, "<a href=\"");
                endLink += 9;
                sb.insert(endLink, "\">");
                endLink += 2;
                sb.insert(endLink, link);
                endLink += link.length();
                sb.insert(endLink, "</a>");
                endLink += 4;
                
                // move currentIndex over and find next link
                currentIndex = endLink;
                temp = sb.toString();
                currentIndex = getNextLinkIndex(temp, currentIndex);
            }
            returnText = sb.toString();
        }
        return returnText;
    }
    
    /**
     * Find the index of the next link with the given starting index
     * @param text
     * @param start
     * @return Index of next link or -1 if none
     */
    private int getNextLinkIndex(String text, int start) {
        String temp = text.toLowerCase();
        int nextLinkStart = -1;
        int httpIndex = temp.indexOf("http://", start);
        int wwwIndex = temp.indexOf("www", start);

        if (httpIndex == -1 && wwwIndex == -1) {
            // no links
            return -1;
        } else {
            // if there is more than one link then we need to check indexes
            // to see which comes first
            if (httpIndex != -1 && wwwIndex == -1) {
                // link only contains http
                nextLinkStart = httpIndex;
            } else if (httpIndex == -1 && wwwIndex != -1) {
                // link only contains www
                nextLinkStart = wwwIndex;
            } else {
                // contains both www and http
                nextLinkStart = httpIndex < wwwIndex ? httpIndex : wwwIndex;
            }
        }
        return nextLinkStart;
    }

    private void sendMessage(boolean action, String messageText) {
        if (serverConnectionProvider != null) {
            serverConnectionProvider.sendMessage(recipient, action, messageText);
        }
    }

    public String getPaneTitle() {
        return recipient;
    }

    public void setFocus() {
        textEntry.requestFocusInWindow();
        textEntry.selectAll();
    }
    /**
     * Creates a Chat Pane
     * @param serverConnectionProvider Provider of server connection
     * @param recipient Recipient of messages coming from this chat pane, either
     * a user name or channel name
     * @param myName 
     * @param channel True if this chat pane is a channel
     */
    public static ChatPane createChatPane(
            ServerConnectionProvider serverConnectionProvider,
            String recipient,
            String myName,
            boolean isChannel) {

        ChatPane channelPane = new ChatPane(serverConnectionProvider,
                recipient, myName, isChannel);
        channelPane.init();
        return channelPane;
    }

    /**
     * Adds a list of participants to this channels list
     * @param participants List of participants in this channel
     */
    public void addParticipants(final List<User> participants) {

        for (User user : participants) {
            userJoinedEvent(user, true);
        }
    }

    /**
     * Handles joining and parting of users in the channel.  If joined
     * is true then the user is added to the channel participant list, otherwise
     * the user is removed from the list.
     * @param user
     * @param joined 
     */
    public void userJoinedEvent(final User user, final boolean joined) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (joined) {
                    if (!participantListModel.contains(user.getName())) {
                        participantListModel.addElement(user.getName());

                        String message = "<span class=\"joinpart\">" + 
                                Util.getTimestamp() + " " + user +
                                " has joined the channel</span><br>";
                        try {
                            htdoc.insertBeforeEnd(chatTextElement, message);
                        } catch (BadLocationException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        } catch (IOException ioe) {
                            logger.log(Level.SEVERE, null, ioe);
                        }
                    }
                } else {
                    participantListModel.removeElement(user.getName());

                    String message = "<span class=\"joinpart\">" + 
                            Util.getTimestamp() + " " + user +
                            " has left the channel</span><br>";
                    try {
                        htdoc.insertBeforeEnd(chatTextElement, message);
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } catch (IOException ioe) {
                            logger.log(Level.SEVERE, null, ioe);
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
    public void userDisconnectedEvent(final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (participantListModel.contains(user.getName())) {
                    participantListModel.removeElement(user.getName());

                    String message = "<span class=\"disconnect\">" + 
                            Util.getTimestamp() + " " + user +
                            " has disconnected from the server</span><br>";
                    try {
                        htdoc.insertBeforeEnd(chatTextElement, message);
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } catch (IOException ioe) {
                        logger.log(Level.SEVERE, null, ioe);
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
                
                try {
                    if (action) {
                        String text = "<span class=\"boldaction\">" + 
                                Util.getTimestamp() + " " + user + ": </span>" + 
                                "<span class=\"action\">" + message + 
                                "</span><br>";
                        
                        htdoc.insertBeforeEnd(chatTextElement, text);
                    } else {
                        String text = "<b>" + Util.getTimestamp() + " " + user + 
                                ": </b>" + convertLinks(message) + "<br>";
                        htdoc.insertBeforeEnd(chatTextElement, text);
                    }
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }  catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Show informational message in the channel window.  This is not a message
     * from a user, but a message related to client or server status.
     * @param message Message to show
     * @param style Text style or null for regular text
     */
    public void showInfoMessage(final String message, final String style) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                try {
                    if (style == null) {
                        // no style applied
                        String text = "<span class=\"bold\">" + 
                                Util.getTimestamp() + ": </span>" + message + "<br>";
                        htdoc.insertBeforeEnd(chatTextElement, text);
                    }
                    if (style.equals("disconnect")) {
                        String text =  "<span class=\"disconnect\">" + 
                                Util.getTimestamp() + ": " + message + "</span>" + "<br>";
                        htdoc.insertBeforeEnd(chatTextElement, text);
                    }
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, null, ioe);
                }
            }
        });
    }
    
    /**
     * Updates the list of users in this channel
     * @param list List of users in the channel
     */
    public void updateUserList(final List<User> list) {
        for (User user : list) {
            userJoinedEvent(user, true);
        }
    }
    
    /**
     * This method should be called after a server disconnect to clean up any 
     * artifacts left from the disconnect.
     */
    public void rejoin() {
        myUserName = serverConnectionProvider.getConnectedUsername();
        if (isChannel) {
            participantListModel.clear();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ThemeManager.TOP_PANE_BACKGROUND)) {
            Color newColor = (Color) evt.getNewValue();
            viewPane.setBackground(newColor);
        } else if (evt.getPropertyName().equals(ThemeManager.TOP_PANE_FOREGROUND)) {
            Color newColor = (Color) evt.getNewValue();
            viewPane.setForeground(newColor);
        }
    }
    
    /**
     * Processes the input message from the chat pane.
     * 
     * @param message The message to process
     */
    private void processInputMessage(String message) {
        
        // Check for an action
        boolean action = message.startsWith(Settings.COMMAND_PREFIX + "me");

        if (!action && isCommand(message)) {
            // Process the command
            processCommand(message);
        } else {
            // If it's an action, remove the command
            if (action) {
                message = message.replaceFirst(Settings.COMMAND_PREFIX + "me", "");
            }
            
            // Display message
            try {
                String insertText = null;
                if (action) {
                    insertText = "<span class=\"boldaction\">"
                            + Util.getTimestamp() + " " + myUserName
                            + ": </span>" + "<span class=\"action\">"
                            + message + "</span><br>";
                } else {
                    insertText = "<b>" + Util.getTimestamp() + " "
                            + myUserName + ": </b>" + convertLinks(message)
                            + "<br>";

                }
                htdoc.insertBeforeEnd(chatTextElement, insertText);
            } catch (BadLocationException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Error appending", ioe);
            }
            sendMessage(action, message);
            textEntry.setText("");
        }
    }
    
    /**
     * Returns true if the specified text is a command 
     * @param text The text to check 
     * @return true if the specified text is a command
     */
    private boolean isCommand(String text) {
        return text.startsWith(Settings.COMMAND_PREFIX);
    }

    /**
     * Processes the command
     * @param command The command to process
     * @return true if the command was processed successfully
     */
    private boolean processCommand(String command) {
        
        boolean success = true;
        
        if (command.startsWith(Settings.COMMAND_PREFIX + "clear")) {
            resetViewPane();
        } else {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("<span class=\"unknowncommand\">");
                sb.append(Util.getTimestamp());
                sb.append(" Unknown command: ");
                sb.append(command.split(" ")[0]);
                sb.append("</span>");
                sb.append("<br>");
                htdoc.insertBeforeEnd(chatTextElement,
                        sb.toString());
            } catch (BadLocationException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, null, ioe);
            }
            success = false;
        }
        
        return success;
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
