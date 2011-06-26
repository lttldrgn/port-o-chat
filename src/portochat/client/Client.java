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
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Brandon
 */
public class Client extends JFrame implements ActionListener, ServerDataListener {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final String EXIT_COMMAND = "EXIT";
    private static final String CONNECT = "CONNECT";
    private static final String CREATE_CHANNEL = "CREATE_CHANNEL";
    private static final String DISCONNECT = "DISCONNECT";
    private HashMap<String, ChatPane> chatPaneMap = 
            new HashMap<String, ChatPane>();
    private HashMap<String, ChatPane> channelPaneMap = 
            new HashMap<String, ChatPane>();
    private DefaultListModel contactListModel = new DefaultListModel();
    private DefaultListModel channelListModel = new DefaultListModel();
    private JList contactList = new JList(contactListModel);
    private JList channelList = new JList(channelListModel);
    private JMenuItem createChannelMenu = new JMenuItem("Create Channel...");
    private JMenuItem disconnect = new JMenuItem("Disconnect");
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
        connectMenu.setMnemonic(KeyEvent.VK_C);
        fileMenu.add(connectMenu);

        createChannelMenu.setActionCommand(CREATE_CHANNEL);
        createChannelMenu.addActionListener(this);
        createChannelMenu.setMnemonic(KeyEvent.VK_H);
        fileMenu.add(createChannelMenu);
        createChannelMenu.setEnabled(false);
        
        disconnect.setMnemonic(KeyEvent.VK_D);
        disconnect.setActionCommand(DISCONNECT);
        disconnect.addActionListener(this);
        disconnect.setEnabled(false);
        fileMenu.add(disconnect);
        
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
                    ChatPane pane = chatPaneMap.get(contact);
                    if (pane == null) {
                        pane = createChatPane(contact);
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
        
        tabbedChatPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int tabIndex = tabbedChatPane.getSelectedIndex();
                setTabColor(tabIndex, Color.black);
            }
        });
    }
    
    /**
     * Joins specified channel if not already joined
     * @param channel Channel to join
     */
    private void joinChannel(String channel) {
                        
        ChatPane pane = channelPaneMap.get(channel);
        if (pane == null) {
            pane = ChatPane.createChatPane(connection, 
                    channel, myUserName, true);

            channelPaneMap.put(channel, pane);
            tabbedChatPane.add(pane.getPaneTitle(), pane);
            tabbedChatPane.setTabComponentAt(
                tabbedChatPane.indexOfComponent(pane), 
                new ButtonTabComponent(tabbedChatPane, this));
            connection.joinChannel(channel);
            connection.requestUsersInChannel(channel);
        }
        tabbedChatPane.setSelectedComponent(pane);
        pane.setFocus();

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
        } else if (e.getActionCommand().equals("CLOSE_TAB")) {
            if (e.getSource() instanceof ButtonTabComponent.TabButton) {
                int i = ((ButtonTabComponent.TabButton) e.getSource()).getComponentIndex();
                String name = tabbedChatPane.getTitleAt(i);
                if (name.startsWith("#")) {
                    connection.partChannel(name);
                    channelPaneMap.remove(name);
                } else {
                    chatPaneMap.remove(name);
                }
                tabbedChatPane.remove(i);
                
            }
        } else if (e.getActionCommand().equals(DISCONNECT)) {
            disconnectFromServer();
        }
    }
    
    /**
     * Connects the client to the server
     * @return 
     */
    private synchronized void connectToServer() {
        if (connected)
            return;
        
        JPanel optionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JTextField userTextField = new JTextField("user");
        JTextField serverTextField = new JTextField("localhost");
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0,0,0,5);
        optionPanel.add(new JLabel("username", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(0,0,0,0);
        optionPanel.add(userTextField, c);
        c.gridx--;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0,0,0,5);
        optionPanel.add(new JLabel("server", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0,0,0,0);
        optionPanel.add(serverTextField, c);
        
        JOptionPane.showMessageDialog(this, optionPanel, "Enter information", JOptionPane.PLAIN_MESSAGE);
        String name = userTextField.getText();
        String server = serverTextField.getText();
        int port = ClientSettings.DEFAULT_SERVER_PORT;
        if (server.contains(":")) {
            String serverArgs[] = server.split(":");
            server = serverArgs[0];
            port = Integer.parseInt(serverArgs[1]);
        }
        
        if (name == null || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid inputs!");
            return;
        }

        try {
            connection = new ServerConnection();
            connection.addDataListener(this);
            connection.connectToServer(server, port);
            connection.sendUsername(name);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not connect!");
            e.printStackTrace();
        }
    }
    
    private synchronized void disconnectFromServer() {
        if (connection == null) {
            return;
        }
        
        connection.disconnect();
        connection.removeDataListener(this);
        connection = null;
        connected = false;
        createChannelMenu.setEnabled(false);
        disconnect.setEnabled(false);
        setTitle("Port-O-Chat - Disconnected");
        contactListModel.clear();
        channelListModel.clear();
    }
    
    @Override
    public void handleServerConnection(final String username, boolean success) {
        if (connected)
            return;
        
        if (success) {
            connection.sendPing();
            connected = true;
            myUserName = username;
            
            connection.sendUserListRequest();
            connection.requestListOfChannels();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createChannelMenu.setEnabled(true);
                    disconnect.setEnabled(true);
                    setTitle("Port-O-Chat: Connected as " + myUserName);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    String name = JOptionPane.showInputDialog(Client.this, 
                        "\"" + username + "\" already in use.  Enter another name", 
                        "Choose another name", JOptionPane.ERROR_MESSAGE);
                    if (name != null) {
                        connection.sendUsername(name);
                    } else {
                        disconnectFromServer();
                    }
                }
            });
        }
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
                
                //Remove from channel lists as well
                Set<String> channelPaneList = channelPaneMap.keySet();
                for (String channel : channelPaneList) {
                    ChatPane pane = channelPaneMap.get(channel);
                    if (pane != null) {
                        pane.userDisconnectedEvent(user);
                    }
                }
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
    
    /**
     * Sets the tab color to the specified color
     * @param index Index of the tab in the tabbed pane
     * @param color 
     */
    private void setTabColor(final int tabIndex, final Color color) {
        // filter out bad indexes and closing tabs
        if ((tabIndex == -1) || (tabIndex + 1 >tabbedChatPane.getTabCount()))
            return;
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final ButtonTabComponent comp = 
                            (ButtonTabComponent)tabbedChatPane.getTabComponentAt(tabIndex);
                comp.setTextColor(color);
                comp.repaint();
                ChatPane chatPane = (ChatPane) tabbedChatPane.getComponentAt(tabIndex);
                chatPane.setFocus();
            }
        });
    }
    
    /**
     * Creates a ChatPane.  Note that it should only be called from the
     * EDT or will throw an error
     * @param userName
     * @return 
     */
    private ChatPane createChatPane(final String userName) {

        assert SwingUtilities.isEventDispatchThread(): "createChatPane called outside EDT";
        ChatPane pane = ChatPane.createChatPane(connection, 
                        userName, myUserName, false);
        chatPaneMap.put(userName, pane);
        tabbedChatPane.add(pane.getPaneTitle(), pane);
        tabbedChatPane.setTabComponentAt(
                tabbedChatPane.indexOfComponent(pane), 
                new ButtonTabComponent(tabbedChatPane, this));
        pane.setFocus();
        return pane;
    }
    
    @Override
    public void userListReceived(final List<String> users, String channel) {
    
        if (channel == null) {
            // this is a server list
            for (String user: users) {
                addUser(user);
            }
        } else {
            ChatPane pane = channelPaneMap.get(channel);
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
    public void receiveChatMessage(final String fromUser, final boolean action, 
        final String message, final String channel) {
        
        // user to user message
        if (channel == null) {
            ChatPane pane = chatPaneMap.get(fromUser);
        
            if (pane == null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        
                        ChatPane pane = createChatPane(fromUser);
                        pane.receiveMessage(fromUser, action, message);
                        int tabIndex = tabbedChatPane.indexOfComponent(pane);
                        if (tabbedChatPane.getSelectedIndex() != tabIndex) {
                            setTabColor(tabIndex, Color.red);
                        }
                    }
                });
            } else {
                // update existing pane
                pane.receiveMessage(fromUser, action, message);
                int tabIndex = tabbedChatPane.indexOfComponent(pane);
                if (tabbedChatPane.getSelectedIndex() != tabIndex) {
                    setTabColor(tabIndex, Color.red);
                }
            }
        } else {
            ChatPane pane = channelPaneMap.get(channel);
            if (pane != null) {
                // update existing pane
                pane.receiveMessage(fromUser, action, message);
                int tabIndex = tabbedChatPane.indexOfComponent(pane);
                if (tabbedChatPane.getSelectedIndex() != tabIndex) {
                    setTabColor(tabIndex, Color.red);
                }
            } else {
                logger.warning("Received a message from a channel that is not joined.");
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
        ChatPane pane = channelPaneMap.get(channel);
        if (pane != null) {
            pane.userJoinedEvent(user, join);
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
