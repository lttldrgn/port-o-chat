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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class SingleChatPane extends JPanel {

    private final static Logger logger =
            Logger.getLogger(SingleChatPane.class.getName());
    private JTextPane viewPane = new JTextPane();
    private JTextArea textEntry = new JTextArea();
    private String recipient = null;
    private String myUserName = null;
    private ServerConnection serverConnection = null;
    private static final SimpleDateFormat formatDate =
            new SimpleDateFormat("h:mm.ssa");

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

        // add listener for text entry
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

        textEntry.requestFocus();

        // add text styles
        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);
        StyledDocument doc = viewPane.getStyledDocument();
        Style s = doc.addStyle("normal", def);
        // bold font
        s = doc.addStyle("bold", def);
        StyleConstants.setBold(s, true);
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

    public String getPaneTitle() {
        return recipient;
    }

    private String getTimestamp() {
        Date currentDate = new Date();
        return formatDate.format(currentDate);
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
