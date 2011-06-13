/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Brandon
 */
public class SingleChatPane extends JPanel {
    private JTextPane viewPane = new JTextPane();
    private JTextArea textEntry = new JTextArea();
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
                    try {
                        doc.insertString(doc.getLength(), 
                                myUserName+": ", doc.getStyle("bold"));
                        doc.insertString(doc.getLength(), 
                                textEntry.getText(), doc.getStyle("normal"));
                    } catch (BadLocationException ex) {
                        Logger.getLogger(SingleChatPane.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    sendMessage(textEntry.getText());
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
    }
    
    private void sendMessage(String messageText) {
        serverConnection.sendMessage(recipient, messageText);
    }

    public String getPaneTitle() {
        return recipient;
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
