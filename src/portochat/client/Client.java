/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *
 * @author Brandon
 */
public class Client extends JFrame {
    ContactListModel contactModel = new ContactListModel();
    private JList contactList = new JList(contactModel);
    private JList channelList = new JList();
 
    public Client(String username) {
        
    }
    
    public void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = getContentPane();
        BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.X_AXIS);
        JPanel leftPane = new JPanel();
        JPanel rightPane = new JPanel();
        contentPane.setLayout(layout);
        contentPane.add(leftPane);
        contentPane.add(rightPane);
        BoxLayout leftPaneLayout = new BoxLayout(leftPane, BoxLayout.PAGE_AXIS);
        leftPane.setLayout(leftPaneLayout);
        leftPane.setPreferredSize(new Dimension(100, 300));
        leftPane.add(new JLabel("Contacts"));
        leftPane.add(new JScrollPane(contactList));
        leftPane.add(new JLabel("Channels"));
        leftPane.add(new JScrollPane(channelList));
        contactModel.addElement("test");
    }
    
    private class ContactListModel extends DefaultListModel {
        List data = new ArrayList();
        @Override
        public void addElement(Object item) {
            data.add(item);
        }
        
        @Override
        public Object getElementAt(int index) {
            return data.get(index);
        }
    }
}
