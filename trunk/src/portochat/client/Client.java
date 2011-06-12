/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author Brandon
 */
public class Client extends JFrame implements ActionListener {
    private static final String EXIT_COMMAND = "EXIT";
    private static final String CONNECT = "CONNECT";
    private DefaultListModel contactListModel = new DefaultListModel();
    private DefaultListModel channelListModel = new DefaultListModel();
    private JList contactList = new JList(contactListModel);
    private JList channelList = new JList(channelListModel);
    private JTabbedPane chatPane = new JTabbedPane(JTabbedPane.BOTTOM, 
            JTabbedPane.SCROLL_TAB_LAYOUT);
    private String myUserName = null;
    private ServerConnection connection = null;
    private boolean connected = false;
 
    public Client(String username) {
        myUserName = username;
        
    }
    
    public void init() {
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // add menu bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        
        JMenuItem connectMenu = new JMenuItem("Connect");
        connectMenu.setActionCommand(CONNECT);
        connectMenu.addActionListener(this);
        fileMenu.add(connectMenu);

        JMenuItem exitMenu = new JMenuItem("Exit");
        exitMenu.setMnemonic(KeyEvent.VK_X);
        exitMenu.setActionCommand(EXIT_COMMAND);
        fileMenu.add(exitMenu);
        exitMenu.addActionListener(this);
        
        // add split pane
        Container contentPane = getContentPane();       
        JPanel leftPane = new JPanel();
        JPanel rightPane = new JPanel();
        JSplitPane splitPane = new 
                JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        splitPane.setDividerLocation(200);
        contentPane.add(splitPane);

        // configure left pane
        // contact list panel
        JPanel contactPanel = new JPanel();
        BoxLayout leftPaneLayout = new BoxLayout(leftPane, BoxLayout.PAGE_AXIS);
        leftPane.setLayout(leftPaneLayout);
        contactPanel.setLayout(new BorderLayout());
        contactPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        contactPanel.setBorder(BorderFactory.createTitledBorder("Contacts"));
        contactPanel.add(new JScrollPane(contactList));
        leftPane.add(contactPanel);
        
        // channel list panel
        JPanel channelPanel = new JPanel();
        channelPanel.setBorder(BorderFactory.createTitledBorder("Channels"));
        channelPanel.setLayout(new BorderLayout());
        channelPanel.add(new JScrollPane(channelList));
        leftPane.add(channelPanel);
        
        // set up listeners
        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    String contact = (String) contactList.getSelectedValue();
                    SingleChatPane pane = SingleChatPane.createChatPane(
                            connection, contact, myUserName);
                    chatPane.add(pane.getPaneTitle(), pane);
                }
            }
        });
        channelList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    System.out.println(channelList.getSelectedValue());
                }
            }
        });
        
        // set up right panel
        rightPane.setLayout(new BorderLayout());
        rightPane.add(chatPane);

    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(EXIT_COMMAND)) {
            int returnVal = JOptionPane.showConfirmDialog(this, "Exit?", 
                    "Exit confirmation", JOptionPane.OK_CANCEL_OPTION);
            if (returnVal == JOptionPane.OK_OPTION) {
                System.exit(0);
            }
        } else if (e.getActionCommand().equals(CONNECT)) {
            connectToServer();
        }
    }
    
    /**
     * Connects the client to the server
     * @return 
     */
    private synchronized boolean connectToServer() {
        if (connected)
            return true;
        
        boolean success = true;
        try {
            connection = new ServerConnection();
            connection.connectToServer("localhost", ClientSettings.DEFAULT_SERVER_PORT);
            connection.sendUsername(myUserName);
            connection.sendPing();
            connected = true;
            // populate fake data
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    contactListModel.addElement("test");
                    contactListModel.addElement("test2");
                    channelListModel.addElement("#channel1");
                }
            });
            
        } catch (Exception e) {
            success = false;
        }
        return success;
    }
    
}
