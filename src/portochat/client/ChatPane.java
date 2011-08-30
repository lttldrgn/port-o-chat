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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import portochat.common.Settings;
import portochat.common.User;

/**
 *
 * @author Brandon
 */
public class ChatPane extends JPanel implements PropertyChangeListener {

    private static final Logger logger =
            Logger.getLogger(ChatPane.class.getName());
    private static final SimpleDateFormat formatDate =
            new SimpleDateFormat("hh:mm.ssa");
    private DefaultListModel participantListModel = null;
    private JList participantList = null;
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

                    boolean action = false;

                    // Check for commands
                    if (text.startsWith(Settings.COMMAND_PREFIX)) {
                        if (text.startsWith(Settings.COMMAND_PREFIX + "me ")) {
                            action = true;
                            text = text.replaceFirst(Settings.COMMAND_PREFIX + "me", "");
                        } else {
                            try {
                                StringBuilder sb = new StringBuilder();
                                sb.append("<span class=\"unknowncommand\">");
                                sb.append(getTimestamp());
                                sb.append(" Unknown command: ");
                                sb.append(text.split(" ")[0]);
                                sb.append("</span>");
                                sb.append("<br>");
                                htdoc.insertBeforeEnd(chatTextElement,
                                        sb.toString());
                            } catch (BadLocationException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            } catch (IOException ioe) {
                                logger.log(Level.SEVERE, null, ioe);
                            }
                        }
                    }

                    // Display message
                    try {
                        if (action) {
                            String insertText = "<span class=\"boldaction\">" + 
                                    getTimestamp() + " " + myUserName + 
                                    ": </span>" + "<span class=\"action\">" + 
                                    text + "</span><br>";
                            htdoc.insertBeforeEnd(chatTextElement, insertText); 
                                    
                        } else {
                            String insertText = "<b>" + getTimestamp() + " " + 
                                    myUserName + ": </b>" + convertLinks(text) + 
                                    "<br>";
                            
                            htdoc.insertBeforeEnd(chatTextElement, insertText);
                        }
                    } catch (BadLocationException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } catch (IOException ioe) {
                        logger.log(Level.SEVERE, "Error appending", ioe);
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
            ArrayList<User> me = new ArrayList<User>();
            me.add(new User(myUserName, "localhost"));
            addParticipants(me);
        }
        initStyles(viewPane);
        ThemeManager.getInstance().addThemeListener(this);
        
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
                                logger.log(Level.SEVERE, null, ex);
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
        startContent.append("</body> </html>");
        
        viewPane.setText(startContent.toString());
        chatTextElement = htdoc.getElement("chatText");
        
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
                                getTimestamp() + " " + user +
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
                            getTimestamp() + " " + user +
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
                            getTimestamp() + " " + user +
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
                                getTimestamp() + " " + user + ": </span>" + 
                                "<span class=\"action\">" + message + 
                                "</span><br>";
                        
                        htdoc.insertBeforeEnd(chatTextElement, text);
                    } else {
                        String text = "<b>" + getTimestamp() + " " + user + 
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
                                getTimestamp() + ": </span>" + message + "<br>";
                        htdoc.insertBeforeEnd(chatTextElement, text);
                    }
                    if (style.equals("disconnect")) {
                        String text =  "<span class=\"disconnect\">" + 
                                getTimestamp() + ": " + message + "</span>" + "<br>";
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

    private String getTimestamp() {
        Date currentDate = new Date();
        return formatDate.format(currentDate);
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
    
    // main for visual test purposes only
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setSize(600, 400);
        frame.getContentPane().add(createChatPane(null, "channel", "bob", true));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
