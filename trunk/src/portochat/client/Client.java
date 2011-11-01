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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import portochat.common.User;

/**
 *
 * @author Brandon
 */
public class Client extends JFrame implements ActionListener, 
        ServerConnectionProvider, ServerDataListener {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final String EXIT_COMMAND = "EXIT";
    private static final String CONNECT = "CONNECT";
    private static final String CREATE_CHANNEL = "CREATE_CHANNEL";
    private static final String DISCONNECT = "DISCONNECT";
    private static final String START_SERVER = "START_SERVER";
    private static final String STATUS_MENU = "STATUS_MENU";
    private static final String THEME_MENU = "THEME_MENU";
    private static final String COMBINED_VIEW = "COMBINED_VIEW";
    private static final String SPLIT_VIEW = "SPLIT_VIEW";
    private HashMap<String, ChatPane> chatPaneMap = 
            new HashMap<String, ChatPane>();
    private HashMap<String, ChatPane> channelPaneMap = 
            new HashMap<String, ChatPane>();
    private DefaultListModel contactListModel = new DefaultListModel();
    private DefaultListModel channelListModel = new DefaultListModel();
    private JDialog chatContainerDialog = null;
    private JList contactList = new JList(contactListModel);
    private JList channelList = new JList(channelListModel);
    private JMenuItem connectMenu = new JMenuItem("Connect");
    private JMenuItem createChannelMenu = new JMenuItem("Create Channel...");
    private JMenuItem disconnect = new JMenuItem("Disconnect");
    private JPanel userChannelContainerPanel = null;
    private JPanel chatContainerPanel = null;
    private JTabbedPane tabbedChatPane = new JTabbedPane(JTabbedPane.BOTTOM, 
            JTabbedPane.SCROLL_TAB_LAYOUT);
    private StatusPane statusPane = null;
    private String myUserName = null;
    private ServerConnection connection = null;
    private boolean connected = false;
    
    // previous state
    private String username = "user";
    private String server = "localhost";
    private int serverPort = ClientSettings.DEFAULT_SERVER_PORT;
    
    // Timer to alert user when a message has been received
    private NotificationTimerListener timerListener = new NotificationTimerListener();
    private Timer notificationTimer = new Timer(1500, timerListener);
    
    private Process serverProcess = null;
    private View currentView = View.COMBINED;
    private enum View {
        COMBINED, SPLIT;
    }
    
    public Client() {
        setTitle("Port-O-Chat");
    }
    
    /**
     * Initializes the GUI and listeners
     */
    public void init() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        // add menu bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        
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
        
        JMenuItem startServer = new JMenuItem("Start Server");
        startServer.setActionCommand(START_SERVER);
        fileMenu.add(startServer);
        startServer.addActionListener(this);
        
        JMenuItem exitMenu = new JMenuItem("Exit");
        exitMenu.setMnemonic(KeyEvent.VK_X);
        exitMenu.setActionCommand(EXIT_COMMAND);
        fileMenu.add(exitMenu);
        exitMenu.addActionListener(this);
        
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(settingsMenu);
        
        JMenuItem themeMenu = new JMenuItem("Change theme");
        themeMenu.addActionListener(this);
        themeMenu.setActionCommand(THEME_MENU);
        settingsMenu.add(themeMenu); 
        
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(viewMenu);
        
        JMenuItem statusMenu = new JMenuItem("Status");
        statusMenu.addActionListener(this);
        statusMenu.setMnemonic(KeyEvent.VK_S);
        statusMenu.setActionCommand(STATUS_MENU);
        viewMenu.add(statusMenu); 
        
        viewMenu.addSeparator();
        ButtonGroup viewGroup = new ButtonGroup();
        JRadioButtonMenuItem combined = new JRadioButtonMenuItem("Combined Layout");
        combined.setSelected(true);
        combined.setActionCommand(COMBINED_VIEW);
        combined.addActionListener(this);
        viewMenu.add(combined);
        viewGroup.add(combined);
        
        JRadioButtonMenuItem split = new JRadioButtonMenuItem("Split Layout");
        split.setActionCommand(SPLIT_VIEW);
        split.addActionListener(this);
        viewGroup.add(split);
        viewMenu.add(split);
        
        // construct panels 
        userChannelContainerPanel = new JPanel();
        chatContainerPanel = new JPanel();

        // configure user/channel pane
        // contact list panel
        JPanel contactPanel = new JPanel();
        contactPanel.setPreferredSize(new Dimension(250, 250));
        contactPanel.setMinimumSize(new Dimension(200, 200));
        BoxLayout leftPaneLayout = 
                new BoxLayout(userChannelContainerPanel, BoxLayout.PAGE_AXIS);
        userChannelContainerPanel.setLayout(leftPaneLayout);
        contactPanel.setLayout(new BorderLayout());
        contactPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        contactPanel.setBorder(BorderFactory.createTitledBorder("Contacts"));
        contactPanel.add(new JScrollPane(contactList));
        userChannelContainerPanel.add(contactPanel);
        
        // channel list panel
        JPanel channelPanel = new JPanel();
        channelPanel.setPreferredSize(new Dimension(250, 250));
        channelPanel.setMinimumSize(new Dimension(200, 200));
        channelPanel.setBorder(BorderFactory.createTitledBorder("Channels"));
        channelPanel.setLayout(new BorderLayout());
        channelPanel.add(new JScrollPane(channelList));
        userChannelContainerPanel.add(channelPanel);
        setCombinedView();
        
        // set up listeners
        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    String contact = (String) contactList.getSelectedValue();
                    if (contact != null) {
                        ChatPane pane = chatPaneMap.get(contact);
                        if (pane == null) {
                            pane = createChatPane(contact);
                            updateCurrentView();
                        }
                        
                        tabbedChatPane.setSelectedComponent(pane);
                    }
                }
            }
        });
        channelList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    String channel = (String) channelList.getSelectedValue();
                    if (channel != null) {
                        joinChannel(channel);
                    }
                }
            }
        });
        
        // set up chat container panel
        chatContainerPanel.setLayout(new BorderLayout());
        tabbedChatPane.setPreferredSize(new Dimension(550, 550));
        chatContainerPanel.add(tabbedChatPane);
        
        tabbedChatPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int tabIndex = tabbedChatPane.getSelectedIndex();
                setTabColor(tabIndex, Color.black);
            }
        });
        
        addWindowFocusListener(timerListener);
        
        statusPane = StatusPane.createStatusPane(this);
        showStatusPane();
        username = GuiUtil.getUserName(this.getClass());
        server = GuiUtil.getServerName(this.getClass());
        serverPort = GuiUtil.getServerPort(this.getClass());
        pack();
    }
    
    private void setCombinedView() {
        getContentPane().removeAll();
        if (chatContainerDialog != null) {
            chatContainerDialog.setVisible(false);
            chatContainerDialog.getContentPane().removeAll();
        }
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                userChannelContainerPanel, chatContainerPanel);
        splitPane.setDividerLocation(250);
        getContentPane().add(splitPane);
        pack();
        currentView = View.COMBINED;
    }
    
    private void setSplitView() {
        getContentPane().removeAll();
        getContentPane().add(userChannelContainerPanel);
        pack();
        
        if (chatContainerDialog == null) {
            chatContainerDialog = new JDialog(this, "Chat");
            chatContainerDialog.setLocation(getX() + getWidth(), getY());
            chatContainerDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            chatContainerDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int openTabs = tabbedChatPane.getTabCount();
                    for (int i = openTabs - 1; i > -1; i--) {
                        ButtonTabComponent comp = (ButtonTabComponent)tabbedChatPane.getTabComponentAt(i);
                        comp.closeTab();
                    }
                    chatContainerDialog.setVisible(false);
                }
            });
            chatContainerDialog.addWindowFocusListener(timerListener);
        }

        chatContainerDialog.getContentPane().add(chatContainerPanel);
        if (tabbedChatPane.getTabCount() > 0) {
            chatContainerDialog.setVisible(true);
        }
        chatContainerDialog.pack();
        currentView = View.SPLIT;
    }
    
    /**
     * Updates the GUI based on the currently selected view
     */
    private void updateCurrentView() {
        if (currentView == View.SPLIT) {
            // make sure dialog is visible
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    chatContainerDialog.setVisible(true);
                }
            });
        }
    }
    
    private void shutdown() {
        if (connected) {
            this.disconnectFromServer();
        }
        GuiUtil.saveUserName(getClass(), username);
        GuiUtil.saveServerName(getClass(), server);
        GuiUtil.saveServerPort(getClass(), serverPort);
        System.exit(0);
    }
    
    /**
     * Makes the status pane visible if it is not
     */
    private void showStatusPane() {
        if (tabbedChatPane.indexOfComponent(statusPane) == -1) {

            tabbedChatPane.add(statusPane.getPaneTitle(), statusPane);
            tabbedChatPane.setTabComponentAt(
                    tabbedChatPane.indexOfComponent(statusPane), 
                    new ButtonTabComponent(tabbedChatPane, this));
            updateCurrentView();
        }
    }
    
    /**
     * Joins specified channel if not already joined
     * @param channel Channel to join
     */
    private void joinChannel(String channel) {
                        
        ChatPane pane = channelPaneMap.get(channel);
        if (pane == null) {
            pane = ChatPane.createChatPane(this, 
                    channel, myUserName, true);

            channelPaneMap.put(channel, pane);
            tabbedChatPane.add(pane.getPaneTitle(), pane);
            tabbedChatPane.setTabComponentAt(
                tabbedChatPane.indexOfComponent(pane), 
                new ButtonTabComponent(tabbedChatPane, this));
            connection.joinChannel(channel);
            connection.requestUsersInChannel(channel);
            updateCurrentView();
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
                shutdown();
            }
        } else if (e.getActionCommand().equals(CONNECT)) {
            connectToServer();
        } else if (e.getActionCommand().equals(CREATE_CHANNEL)) {
            String returnVal = JOptionPane.showInputDialog(rootPane, 
                    "Enter channel name to create", "Channel creation", 
                    JOptionPane.QUESTION_MESSAGE);
            if (returnVal != null) {
                if (returnVal.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                            "Invalid name", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
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
                    if (connected) {
                        connection.partChannel(name);
                    }
                    channelPaneMap.remove(name);
                } else {
                    chatPaneMap.remove(name);
                }
                tabbedChatPane.remove(i);

                if (tabbedChatPane.getTabCount() == 0 && currentView == View.SPLIT) {
                    chatContainerDialog.setVisible(false);
                }
            }
        } else if (e.getActionCommand().equals(DISCONNECT)) {
            disconnectFromServer();
        } else if (e.getActionCommand().equals(STATUS_MENU)) {
            showStatusPane();
        } else if (e.getActionCommand().equals(THEME_MENU)) {
            ThemeManager themeManager = ThemeManager.getInstance();
            themeManager.setTopLevelComponent(this);
            themeManager.setVisible(true);
        } else if (e.getActionCommand().equals(START_SERVER)) {
            startServer();
        } else if (e.getActionCommand().equals(SPLIT_VIEW)) {
            setSplitView();
        } else if (e.getActionCommand().equals(COMBINED_VIEW)) {
            setCombinedView();
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
        JTextField userTextField = new JTextField(username, 10);
        JTextField serverTextField = new JTextField(server);
        JTextField serverPortTextField = new JTextField(Integer.toString(serverPort));
        
        // user name
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(1,1,1,5);
        optionPanel.add(new JLabel("username", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_END;
        optionPanel.add(userTextField, c);
        
        // server name
        c.gridx--;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        optionPanel.add(new JLabel("server", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        optionPanel.add(serverTextField, c);
        
        // server port
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        optionPanel.add(new JLabel("port", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        optionPanel.add(serverPortTextField, c);
        
        // add focus selection listener
        FocusAdapter focusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (e.getComponent() instanceof JTextField) {
                    JTextField entry = (JTextField) e.getComponent();
                    entry.selectAll();
                }
            }
        };
        userTextField.addFocusListener(focusListener);
        serverTextField.addFocusListener(focusListener);
        serverPortTextField.addFocusListener(focusListener);
        
        int returnVal = JOptionPane.showConfirmDialog(this, optionPanel, 
                "Enter information", JOptionPane.OK_CANCEL_OPTION);
        if (returnVal != JOptionPane.OK_OPTION) {
            return;
        }
        username = userTextField.getText().trim();
        server = serverTextField.getText().trim();
        try {
            serverPort = Integer.parseInt(serverPortTextField.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid port");
            return;
        }
        
        if (username == null || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid inputs!");
            return;
        }

        boolean success = false;
        try {
            connection = new ServerConnection();
            connection.addDataListener(this);
            success = connection.connectToServer(server, serverPort);
            connection.sendUsername(username);
            
        } catch (UnknownHostException e) {
            statusPane.showMessage("Connection failed because of unknown " +
                    "server name.  Please check the server name.", null);
        } catch (ConnectException e) {
            statusPane.showMessage("Connection failed.  Please check that the " +
                    "server is running and that the client is configured " +
                    "properly (i.e. server port number is correct).", null);
            statusPane.showMessage("Error message: " + e.getMessage(), null);
        } catch (Exception e) {
            statusPane.showMessage("Unknown error trying to connect to server. See log.", null);
            logger.log(Level.SEVERE, "Unknown error.  Please report this issue.", e);
        }
        if (!success) {
            JOptionPane.showMessageDialog(this, "Could not connect.  See status pane for reason");
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
        connectMenu.setEnabled(true);
        createChannelMenu.setEnabled(false);
        disconnect.setEnabled(false);
        setTitle("Port-O-Chat - Disconnected");
        contactListModel.clear();
        channelListModel.clear();
        showDisconnectMessage();
        notificationTimer.stop();
        timerListener.reset();
    }
    
    private void showDisconnectMessage() {
        Collection<ChatPane> chatPanes = chatPaneMap.values();
        for (ChatPane pane : chatPanes) {
            pane.showInfoMessage("Disconnected from server", "disconnect");
        }
        
        Collection<ChatPane> channelPanes = channelPaneMap.values();
        for (ChatPane pane : channelPanes) {
            pane.showInfoMessage("Disconnected from server", "disconnect");
        }
        statusPane.showMessage("Disconnected from server", "disconnect");
    }
    
    private void startServer() {
        if (serverProcess != null) {
            return;
        }
    
        final ProcessBuilder pb = new ProcessBuilder("java", "-cp", 
                "PortOChat.jar", "portochat.server.ServerLauncherGUI");
            
        // get current working directory so we can find the jar
        File f = new File(".");
        String jarDirectory = "."; // PortOChat.jar directory location
        try {
            jarDirectory = f.getCanonicalPath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        // check for PortOChat.jar
        File jarFile = new File(jarDirectory + File.separator + "PortOChat.jar");
        if (!jarFile.exists()) {
            // allow user to select directory where PortOChat.jar resides
            JOptionPane.showMessageDialog(this, "Could not find PortOChat.jar.  Select directory where it resides.");
            JFileChooser chooser = new JFileChooser(jarDirectory);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    jarDirectory = chooser.getSelectedFile().getCanonicalPath();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Error getting path", ex);
                }
            } else {
                // user canceled so abort
                return;
            }
        }
        
        // get the current directory
        pb.directory(new File(jarDirectory));

        try {
            pb.redirectErrorStream(true);
            serverProcess = pb.start();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error starting Server", ex);
        }
        if (serverProcess == null) {
            JOptionPane.showMessageDialog(this, "Couldn't launch process.  See log for error");
            return;
        }
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(serverProcess.getInputStream()));

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException ex) {
                    logger.log(Level.INFO, "IOException reading stream.", ex);
                }
                serverProcess = null;
            }
        }, "Process Drainer");
        t.start();
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
                    connectMenu.setEnabled(false);
                    createChannelMenu.setEnabled(true);
                    disconnect.setEnabled(true);
                    setTitle("Port-O-Chat: Connected as " + myUserName);
                    rejoinOpenChannels();
                    if (notificationTimer.isRunning()) {
                        notificationTimer.restart();
                    } else {
                        notificationTimer.start();
                    }
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

    private void addUser(final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!contactListModel.contains(user.getName()))
                    contactListModel.addElement(user.getName());
            }
        });
    }
    
    private void removeUser(final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (user == null) {
                    disconnectFromServer();
                    return;
                }
                
                if (contactListModel.contains(user.getName()))
                    contactListModel.removeElement(user.getName());
                
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
        if (SwingUtilities.isEventDispatchThread()) {
            // filter out bad indexes and closing tabs
            if ((tabIndex == -1) || (tabIndex + 1 >tabbedChatPane.getTabCount()))
                return;
            final ButtonTabComponent comp = 
                        (ButtonTabComponent)tabbedChatPane.getTabComponentAt(tabIndex);
            if (comp != null) {
                // on tab create the button component has not yet been set
                // so only set the color later when it is no longer null
                comp.setTextColor(color);
                comp.repaint();
            }
            Component pane = tabbedChatPane.getComponentAt(tabIndex);
            if (pane instanceof ChatPane) {
                ((ChatPane)pane).setFocus();
            }
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setTabColor(tabIndex, color);
                }
            });
        }
    }
    
    /**
     * Creates a ChatPane.  Note that it should only be called from the
     * EDT or will throw an error
     * @param userName
     * @return 
     */
    private ChatPane createChatPane(final String userName) {

        assert SwingUtilities.isEventDispatchThread(): "createChatPane called outside EDT";
        ChatPane pane = ChatPane.createChatPane(this, 
                        userName, myUserName, false);
        chatPaneMap.put(userName, pane);
        tabbedChatPane.add(pane.getPaneTitle(), pane);
        tabbedChatPane.setTabComponentAt(
                tabbedChatPane.indexOfComponent(pane), 
                new ButtonTabComponent(tabbedChatPane, this));
        pane.setFocus();
        return pane;
    }
    
    /**
     * This method will rejoin any open channels after a server disconnection
     * has occurred.
     */
    private void rejoinOpenChannels() {
        Set<Entry<String, ChatPane>> channelEntries = channelPaneMap.entrySet();
        for (Entry<String, ChatPane> entry : channelEntries) {
            entry.getValue().rejoin();
            connection.joinChannel(entry.getKey());
            connection.requestUsersInChannel(entry.getKey());
        }
    }
    
    @Override
    public void userListReceived(final List<User> users, String channel) {
    
        if (channel == null) {
            // this is a server list
            for (User user: users) {
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
    public void userConnectionEvent(User user, boolean connected) {
        if (connected) {
            addUser(user);
        } else {
            removeUser(user);
        }
    }

    @Override
    public void receiveChatMessage(final User fromUser, final boolean action, 
        final String message, final String channel) {

        // user to user message
        if (channel == null) {
            ChatPane pane = chatPaneMap.get(fromUser.getName());
        
            if (pane == null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        
                        ChatPane pane = createChatPane(fromUser.getName());
                        pane.receiveMessage(fromUser.getName(), action, message);
                        int tabIndex = tabbedChatPane.indexOfComponent(pane);
                        if (tabbedChatPane.getSelectedIndex() != tabIndex) {
                            setTabColor(tabIndex, Color.red);
                        }
                        timerListener.notifyMessageReceived();
                        updateCurrentView();
                    }
                });
            } else {
                // update existing pane
                pane.receiveMessage(fromUser.getName(), action, message);
                int tabIndex = tabbedChatPane.indexOfComponent(pane);
                if (tabbedChatPane.getSelectedIndex() != tabIndex) {
                    setTabColor(tabIndex, Color.red);
                }
                timerListener.notifyMessageReceived();
            }
        } else {
            ChatPane pane = channelPaneMap.get(channel);
            if (pane != null) {
                // update existing pane
                pane.receiveMessage(fromUser.getName(), action, message);
                int tabIndex = tabbedChatPane.indexOfComponent(pane);
                if (tabbedChatPane.getSelectedIndex() != tabIndex) {
                    setTabColor(tabIndex, Color.red);
                }
                timerListener.notifyMessageReceived();
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
    public void receiveChannelJoinPart(User user, String channel,
        boolean join) {
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

    @Override
    public void sendMessage(String recipient, boolean action, String message) {
        if (connection != null) {
            connection.sendMessage(recipient, action, message);
        }
    }

    @Override
    public String getConnectedUsername() {
        return myUserName;
    }
    
    private class NotificationTimerListener implements ActionListener, 
            WindowFocusListener {
        
        private String currentTitle;
        private String dialogTitle = "Chat";
        private boolean isWindows = false;
        private volatile boolean messageReceived = false;
        
        public NotificationTimerListener() {
            isWindows = System.getProperty("os.name").contains("Windows");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentTitle == null) {
                currentTitle = getTitle();
            }
            
            if (messageReceived) {
                // if the window is not active and a message is received
                // set the title to give a visual cue to user
                if (currentView == View.COMBINED) {
                    if (getTitle().equals(currentTitle)) {
                        setTitle("Message received " + currentTitle);
                    } else {
                        setTitle(currentTitle);
                    }
                } else {
                    // split view, update dialog instead
                    if (chatContainerDialog.getTitle().equals(dialogTitle)) {
                        chatContainerDialog.setTitle("Message received");
                    } else {
                        chatContainerDialog.setTitle(dialogTitle);
                    }
                }
            }
        }
        
        public void notifyMessageReceived() {
            if (currentView == View.COMBINED) {
                if (!Client.this.isActive()) {
                    messageReceived = true;
                    if (isWindows) {
                        // flash taskbar icon in windows
                        toFront();
                    }
                }
            } else {
                // split view
                if (!chatContainerDialog.isActive()) {
                    messageReceived = true;
                    if (isWindows) {
                        // flash taskbar icon in windows
                        chatContainerDialog.toFront();
                    }
                }
            }
        }
        
        public void reset() {
            currentTitle = null;
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
            
            // handle the case where the title has been reset but user has not
            // regained focus
            if (e.getSource().equals(Client.this) && currentView == View.COMBINED) {
                messageReceived = false;
                if (currentTitle != null) {
                    setTitle(currentTitle);
                }
            } else if (e.getSource().equals(chatContainerDialog)) {
                // split view, update dialog instead
                messageReceived = false;
                chatContainerDialog.setTitle(dialogTitle);
            }
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            
        }
        
    }
}
