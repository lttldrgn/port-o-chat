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

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import portochat.common.User;
import portochat.common.Util;
import java.util.ResourceBundle;

/**
 *
 * @author Brandon
 */
public class StatusPane extends JPanel {

    private static final Logger logger =
            Logger.getLogger(StatusPane.class.getName());
    private JTextPane viewPane = new JTextPane();
    private ServerConnectionProvider serverConnectionProvider = null;

    /**
     * Creates a Chat Pane
     * @param serverProvider Provider of the server connection
     * @param recipient Recipient of messages coming from this chat pane, either
     * a user name or channel name
     * @param myName 
     * @param channel True if this chat pane is a channel
     */
    private StatusPane(ServerConnectionProvider serverProvider) {
        serverConnectionProvider = serverProvider;
    }

    private void init() {
        // construct GUI
        setLayout(new BorderLayout());
        add(new JScrollPane(viewPane), BorderLayout.CENTER);
        viewPane.setEditable(false);

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
        StyleConstants.setForeground(s, new Color(0, 153, 0));
        s = doc.addStyle("disconnect", def);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, new Color(0, 0, 153));
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

    public String getPaneTitle() {
        ResourceBundle messages = ResourceBundle.getBundle("portochat/resource/MessagesBundle", java.util.Locale.getDefault());
        return messages.getString("StatusPane.msg.Status");
    }
    
    /**
     * Creates a Chat Pane
     * @param serverConnectionProvider Provider of server connection
     */
    public static StatusPane createStatusPane(
            ServerConnectionProvider serverConnectionProvider) {

        StatusPane statusPane = new StatusPane(serverConnectionProvider);
        statusPane.init();
        return statusPane;
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
                ResourceBundle messages = ResourceBundle.getBundle("portochat/resource/MessagesBundle", java.util.Locale.getDefault());

                StyledDocument doc = viewPane.getStyledDocument();
                String message = Util.getTimestamp() + " " + user
                        + messages.getString("StatusPane.msg.HasDisconnectedFromTheServer");
                try {
                    doc.insertString(doc.getLength(), message,
                            doc.getStyle("disconnect"));
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Show informational message in the status window.
     * @param message Message to show
     * @param style Text style or null for regular text
     */
    public void showMessage(final String message, final String style) {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                StyledDocument doc = viewPane.getStyledDocument();
                try {
                    if (style == null) {
                        doc.insertString(doc.getLength(),
                                Util.getTimestamp() + ": ",
                                doc.getStyle("bold"));
                        doc.insertString(doc.getLength(),
                                message + "\n",
                                doc.getStyle("normal"));
                    } else if (style.equals("disconnect")) {
                        doc.insertString(doc.getLength(), 
                                    Util.getTimestamp() + ": " + message + "\n",
                                    doc.getStyle("disconnect"));
                    }
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
