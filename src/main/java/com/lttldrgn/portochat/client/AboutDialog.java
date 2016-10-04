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
package com.lttldrgn.portochat.client;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import com.lttldrgn.portochat.client.util.VersionChecker;
import com.lttldrgn.portochat.common.VersionInfo;

/**
 * Simple About dialog with information about the program
 * 
 */
public class AboutDialog extends JDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(AboutDialog.class.getName());
    private static final String aboutPortochat = "Port-O-Chat is licensed under " +
            "<a href=\"http://www.gnu.org/licenses/gpl-3.0.html\">GPL3</a>.<br> " +
            "<br>The source and latest version of Port-O-Chat can be found at:<br>" +
            "<a href=\"https://github.com/lttldrgn/port-o-chat\">" + 
            "https://github.com/lttldrgn/port-o-chat</a>";
    
    private ResourceBundle messages = ResourceBundle.getBundle("portochat/resource/MessagesBundle", java.util.Locale.getDefault());
    private final JButton close = new JButton("Close");
    private final JButton checkUpdate = new JButton("Check for update");
    
    private AboutDialog(JFrame parent) {
        super(parent, "About Port-O-Chat");
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        final JTextPane textPane = new JTextPane();
        textPane.setEditorKit(new HTMLEditorKit());
        textPane.setText(getAboutText());
        textPane.setEditable(false);
        content.add(textPane, BorderLayout.CENTER);
//        content.add(checkUpdate, BorderLayout.SOUTH);
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(close, BorderLayout.SOUTH);
        close.addActionListener(this);
        checkUpdate.addActionListener(this);
        pack();
        
        textPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            try {
                                desktop.browse(URI.create(e.getDescription()));
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(textPane, 
                                        messages.getString("ChatPane.msg.CouldNotLaunchDefaultBrowserSeeLogForReason"));
                                logger.log(Level.INFO, 
                                        messages.getString("ChatPane.msg.CouldNotLaunchBrowser"), ex);
                            }
                        }
                    }
                }
            }
            
        });
    }
    
    /**
     * Show the about dialog
     * @param parent Parent frame to attach to
     */
    public static void showAboutDialog(JFrame parent) {
        AboutDialog dialog = new AboutDialog(parent);
        dialog.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(close)) {
            this.dispose();
        } else if (e.getSource().equals(checkUpdate)) {
            checkForNewerVersion();
        }
    }
    
    /**
     * Checks for a newer version in the background and notifies user if 
     * software is up to date.
     */
    private void checkForNewerVersion() {
        VersionChecker.checkVersion(new VersionChecker.VersionResultCallback() {

            @Override
            public void onResult(VersionChecker.VersionResultEnum result) {
                switch(result) {
                    case CONNECTION_ERROR:
                        JOptionPane.showMessageDialog(AboutDialog.this, 
                                "Can not check for updates at this time.  Please check your internet connection");
                        break;
                    case OUT_OF_DATE:
                        JOptionPane.showMessageDialog(AboutDialog.this, "A newer version is available");
                        break;
                    case UP_TO_DATE:
                        JOptionPane.showMessageDialog(AboutDialog.this, "Software is up to date.");
                        break;
                }
            }
        });
    }
    
    private String getAboutText() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("Running version: ");
        sb.append(VersionInfo.CURRENT_VERSION);
        sb.append("<br>");
        sb.append(aboutPortochat);
        sb.append("</html>");
        return sb.toString();
    }
    
    
}
