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
import java.util.HashMap;
import java.util.List;
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
public class Client extends JFrame implements ActionListener, ServerDataListener {
    private static final String EXIT_COMMAND = "EXIT";
    private static final String CONNECT = "CONNECT";
    private static final String CREATE_CHANNEL = "CREATE_CHANNEL";
    private HashMap<String, SingleChatPane> chatPaneMap = 
            new HashMap<String, SingleChatPane>();
    private HashMap<String, ChannelPane> channelPaneMap = 
            new HashMap<String, ChannelPane>();
    private DefaultListModel contactListModel = new DefaultListModel();
    private DefaultListModel channelListModel = new DefaultListModel();
    private JList contactList = new JList(contactListModel);
    private JList channelList = new JList(channelListModel);
    private JMenuItem createChannelMenu = new JMenuItem("Create Channel...");
    private JTabbedPane tabbedChatPane = new JTabbedPane(JTabbedPane.BOTTOM, 
            JTabbedPane.SCROLL_TAB_LAYOUT);
    private String myUserName = null;
    private ServerConnection connection = null;
    private boolean connected = false;
 
    public Client() {
        setTitle("Port-O-Chat");
    }
    
    /**
     * Initializes the GUI and listeners
     */
    public void init() {
        setSize(1024, 768);
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

        createChannelMenu.setActionCommand(CREATE_CHANNEL);
        createChannelMenu.addActionListener(this);
        fileMenu.add(createChannelMenu);
        createChannelMenu.setEnabled(false);
        
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
                    SingleChatPane pane = chatPaneMap.get(contact);
                    if (pane == null) {
                        pane = SingleChatPane.createChatPane(
                                connection, contact, myUserName);
                        chatPaneMap.put(contact, pane);
                        tabbedChatPane.add(pane.getPaneTitle(), pane);
                    }
                    tabbedChatPane.setSelectedComponent(pane);
                }
            }
        });
        channelList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    String channel = (String) channelList.getSelectedValue();
                    joinChannel(channel);
                }
            }
        });
        
        // set up right panel
        rightPane.setLayout(new BorderLayout());
        rightPane.add(tabbedChatPane);

    }
    
    /**
     * Joins specified channel if not already joined
     * @param channel Channel to join
     */
    private void joinChannel(String channel) {
                        
        ChannelPane pane = channelPaneMap.get(channel);
        if (pane == null) {
            pane = ChannelPane.createChannelPane(connection, 
                    channel, myUserName);

            channelPaneMap.put(channel, pane);
            tabbedChatPane.add(pane.getPaneTitle(), pane);
            connection.joinChannel(channel);
            connection.requestUsersInChannel(channel);
        }
        tabbedChatPane.setSelectedComponent(pane);

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
        } else if (e.getActionCommand().equals(CREATE_CHANNEL)) {
            String returnVal = JOptionPane.showInputDialog(rootPane, 
                    "Enter channel name to create", "Channel creation", 
                    JOptionPane.QUESTION_MESSAGE);
            if (returnVal != null) {
                String channel = returnVal;
                if (!channel.startsWith("#"))
                    channel = "#" + channel;
                joinChannel(channel);
            }
        }
    }
    
    /**
     * Connects the client to the server
     * @return 
     */
    private synchronized boolean connectToServer() {
        if (connected)
            return true;
        
        String name = JOptionPane.showInputDialog("Enter your user name");
        if (name == null || name.isEmpty())
            return false;
        
        myUserName = name;
        boolean success = true;
        try {
            connection = new ServerConnection();
            connection.addDataListener(this);
            connection.connectToServer("localhost", ClientSettings.DEFAULT_SERVER_PORT);
            connection.sendUsername(myUserName);
            connection.sendPing();
            connected = true;
            createChannelMenu.setEnabled(true);
            setTitle("Port-O-Chat: Connected as " + myUserName);
            
            connection.sendUserListRequest();
            connection.joinChannel("#test");
            connection.joinChannel("#test2");
            //connection.partChannel("#test");
            //connection.sendMessage("#test", "Did you know that");
            //connection.sendMessage("#test2", "The bird is the word");
            connection.requsetListOfChannels();
            connection.requestUsersInChannel("#test2");
            
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    private void addUser(final String user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!contactListModel.contains(user))
                    contactListModel.addElement(user);
            }
        });
    }
    
    private void removeUser(final String user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (contactListModel.contains(user))
                    contactListModel.removeElement(user);
            }
        });
    }
    
    private void addChannelToList(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!channelListModel.contains(channel))
                    channelListModel.addElement(channel);
            }
        });
    }
    
    private void removeChannelFromList(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channelListModel.removeElement(channel);
            }
        });
    }
    
    @Override
    public void userListReceived(final List<String> users, String channel) {
    
        if (channel == null) {
            // this is a server list
            for (String user: users) {
                addUser(user);
            }
        } else {
            ChannelPane pane = channelPaneMap.get(channel);
            if (pane != null) {
                pane.updateUserList(users);
            }
        }
    }
    
    @Override
    public void userConnectionEvent(String user, boolean connected) {
        if (connected) {
            addUser(user);
        } else {
            removeUser(user);
        }
    }

    @Override
    public void receiveChatMessage(final String fromUser, final String message, 
            final String channel) {
        
        // user to user message
        if (channel == null) {
            SingleChatPane pane = chatPaneMap.get(fromUser);
        
            if (pane == null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        SingleChatPane pane = SingleChatPane.createChatPane(connection, 
                                        fromUser, myUserName);
                        chatPaneMap.put(fromUser, pane);
                        tabbedChatPane.add(pane.getPaneTitle(), pane);
                        pane.receiveMessage(fromUser, message);
                    }
                });
            } else {
                // update existing pane
                pane.receiveMessage(fromUser, message);
            }
        } else {
            ChannelPane pane = channelPaneMap.get(channel);
            // TODO: If user has not joined a channel then should not create
            // a panel if an unsubscribed message is received from it
            if (pane == null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ChannelPane pane = ChannelPane.createChannelPane(connection, 
                                        channel, myUserName);
                        channelPaneMap.put(channel, pane);
                        tabbedChatPane.add(pane.getPaneTitle(), pane);
                        pane.receiveMessage(fromUser, message);
                    }
                });
            } else {
                // update existing pane
                pane.receiveMessage(fromUser, message);
            }
        }

    }

    @Override
    public void channelListReceived(List<String> channels) {
        for (String channel : channels) {
            addChannelToList(channel);
        }
    }

    @Override
    public void receiveChannelJoinPart(String user, String channel, boolean join) {
        ChannelPane pane = channelPaneMap.get(channel);
        if (pane != null) {
            pane.userConnectionEvent(user, join);
        }
    }

    @Override
    public void channelStatusReceived(final String channel, boolean created) {
        if (created) {
            addChannelToList(channel);
        } else {
            removeChannelFromList(channel);
        }
    }
}
