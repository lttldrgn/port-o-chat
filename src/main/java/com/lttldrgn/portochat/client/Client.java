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
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
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
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.lttldrgn.portochat.client.util.VersionChecker;
import com.lttldrgn.portochat.common.User;
import com.lttldrgn.portochat.server.ServerLauncherGUI;

/**
 *
 * @author Brandon
 */
public class Client extends JFrame implements ActionListener, 
        ServerConnectionProvider, ServerDataListener, UserEventListener {
    private final ResourceBundle messages = ResourceBundle.getBundle("portochat/resource/MessagesBundle", java.util.Locale.getDefault());
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
    private static final String SHOW_ABOUT_DIALOG = "SHOW_ABOUT_DIALOG";
    private final HashMap<String, ChatPane> chatPaneMap = new HashMap<>();
    private final HashMap<String, ChatPane> channelPaneMap = new HashMap<>();
    private final DefaultListModel<String> contactListModel = new DefaultListModel<>();
    private final DefaultListModel<String> channelListModel = new DefaultListModel<>();
    private JDialog chatContainerDialog = null;
    private final JList<String> contactList = new JList<>(contactListModel);
    private final JList<String> channelList = new JList<>(channelListModel);
    private final JMenuItem connectMenu = new JMenuItem(messages.getString("Client.menu.Connect"));
    private final JMenuItem createChannelMenu = new JMenuItem(messages.getString("Client.menu.CreateChannel"));
    private final JMenuItem disconnect = new JMenuItem(messages.getString("Client.menu.Disconnect"));
    private JPanel userChannelContainerPanel;
    private JPanel chatContainerPanel;
    private final JTabbedPane tabbedChatPane = new JTabbedPane(JTabbedPane.BOTTOM, 
            JTabbedPane.SCROLL_TAB_LAYOUT);
    private StatusPane statusPane = null;
    private String myUserName = null;
    private ServerConnection connection = null;
    private final ServerDataStorage serverData;
    private boolean connected = false;
    
    // previous state
    private volatile String username = messages.getString("Client.defaultvalue.User");
    private String server = "localhost";
    private int serverPort = ClientSettings.DEFAULT_SERVER_PORT;
    
    // Timer to alert user when a message has been received
    private final NotificationTimerListener timerListener = new NotificationTimerListener();
    private final Timer notificationTimer = new Timer(1500, timerListener);
    
    private View currentView = View.COMBINED;
    private enum View {
        COMBINED, SPLIT;
    }
    
    public Client() {
        this.userChannelContainerPanel = null;
        setTitle(messages.getString("Client.title.PortOChat"));
        serverData = ServerDataStorage.getInstance();
        serverData.addUserEventListener(this);
    }
    
    public void checkVersion() {
        VersionChecker.checkVersion((VersionChecker.VersionResultEnum result) -> {
            switch(result) {
                case OUT_OF_DATE:
                    JOptionPane.showMessageDialog(Client.this, "A newer version is available");
                    break;
            }
        });
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
        
        createMenuBar();
        
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
        contactPanel.setBorder(BorderFactory.createTitledBorder(messages.getString("Client.title.Contacts")));
        contactPanel.add(new JScrollPane(contactList));
        userChannelContainerPanel.add(contactPanel);
        
        // channel list panel
        JPanel channelPanel = new JPanel();
        channelPanel.setPreferredSize(new Dimension(250, 250));
        channelPanel.setMinimumSize(new Dimension(200, 200));
        channelPanel.setBorder(BorderFactory.createTitledBorder(messages.getString("Client.title.Channels")));
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
                        showUserChatPane(contact);
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
        
        tabbedChatPane.addChangeListener((ChangeEvent e) -> {
            int tabIndex = tabbedChatPane.getSelectedIndex();
            setTabColor(tabIndex, Color.black);
        });
        
        addWindowFocusListener(timerListener);
        
        statusPane = StatusPane.createStatusPane(this);
        showStatusPane();
        username = GuiUtil.getUserName(this.getClass());
        server = GuiUtil.getServerName(this.getClass());
        serverPort = GuiUtil.getServerPort(this.getClass());
        pack();
    }
    
    private void createMenuBar() {
        
        // add menu bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu fileMenu = new JMenu(messages.getString("Client.menu.File"));
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
        
        JMenuItem startServer = new JMenuItem(messages.getString("Client.menu.StartServer"));
        startServer.setActionCommand(START_SERVER);
        fileMenu.add(startServer);
        startServer.addActionListener(this);
        
        JMenuItem exitMenu = new JMenuItem(messages.getString("Client.menu.Exit"));
        exitMenu.setMnemonic(KeyEvent.VK_X);
        exitMenu.setActionCommand(EXIT_COMMAND);
        fileMenu.add(exitMenu);
        exitMenu.addActionListener(this);
        
        JMenu settingsMenu = new JMenu(messages.getString("Client.menu.Settings"));
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(settingsMenu);
        
        JMenuItem themeMenu = new JMenuItem(messages.getString("Client.menu.ChangeTheme"));
        themeMenu.addActionListener(this);
        themeMenu.setActionCommand(THEME_MENU);
        settingsMenu.add(themeMenu); 
       
        JMenu viewMenu = new JMenu(messages.getString("Client.menu.View"));
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(viewMenu);
        
        JMenuItem statusMenu = new JMenuItem(messages.getString("Client.menu.Status"));
        statusMenu.addActionListener(this);
        statusMenu.setMnemonic(KeyEvent.VK_S);
        statusMenu.setActionCommand(STATUS_MENU);
        viewMenu.add(statusMenu); 
        
        viewMenu.addSeparator();
        ButtonGroup viewGroup = new ButtonGroup();
        JRadioButtonMenuItem combined = new JRadioButtonMenuItem(messages.getString("Client.menu.CombinedLayout"));
        combined.setSelected(true);
        combined.setActionCommand(COMBINED_VIEW);
        combined.addActionListener(this);
        viewMenu.add(combined);
        viewGroup.add(combined);
        
        JRadioButtonMenuItem split = new JRadioButtonMenuItem(messages.getString("Client.menu.SplitLayout"));
        split.setActionCommand(SPLIT_VIEW);
        split.addActionListener(this);
        viewGroup.add(split);
        viewMenu.add(split);
        
        JMenu helpMenu = new JMenu(messages.getString("Client.menu.Help"));
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);
        
        JMenuItem about = new JMenuItem(messages.getString("Client.menu.About"));
        about.setActionCommand(SHOW_ABOUT_DIALOG);
        about.addActionListener(this);
        about.setMnemonic(KeyEvent.VK_A);
        helpMenu.add(about);
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
            chatContainerDialog = new JDialog(this, messages.getString("Client.title.Chat"));
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
            SwingUtilities.invokeLater(() -> {
                chatContainerDialog.setVisible(true);
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
     * Show and bring into focus the chat pane for the contact.  Will be created
     * if it does not exist.  Method should always be called from the EDT.
     * @param contact User contact name
     */
    void showUserChatPane(String contact) {
        ChatPane pane = chatPaneMap.get(contact);
        if (pane == null) {
            pane = createChatPane(contact);
            updateCurrentView();
        }

        tabbedChatPane.setSelectedComponent(pane);
        pane.setFocus();
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
            int returnVal = JOptionPane.showConfirmDialog(this, messages.getString("Client.msg.Exit"), 
                    messages.getString("Client.title.ExitConfirmation"), JOptionPane.OK_CANCEL_OPTION);
            if (returnVal == JOptionPane.OK_OPTION) {
                shutdown();
            }
        } else if (e.getActionCommand().equals(CONNECT)) {
            connectToServer();
        } else if (e.getActionCommand().equals(CREATE_CHANNEL)) {
            JTextField channelName = new JTextField();
            channelName.setDocument(new FilteredPlainDoc("\\w*", 16));
            Object msg[] = {messages.getString("Client.msg.EnterChannelNameToCreate"), channelName};
            int returnVal = JOptionPane.showConfirmDialog(rootPane, 
                    msg, messages.getString("Client.title.ChannelCreation"), 
                    JOptionPane.OK_CANCEL_OPTION);
            if (returnVal == JOptionPane.OK_OPTION) {
                if (channelName.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                            messages.getString("Client.msg.InvalidName"), 
                            messages.getString("Client.title.Error"), 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String channel = channelName.getText();
                if (!channel.startsWith("#"))
                    channel = "#" + channel;
                joinChannel(channel);
            }
        } else if (e.getActionCommand().equals(ButtonTabComponent.CLOSE_TAB)) {
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
        } else if (e.getActionCommand().equals(SHOW_ABOUT_DIALOG)) {
            AboutDialog.showAboutDialog(this);
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
        
        JTextField userTextField = new JTextField(new FilteredPlainDoc("\\w*",16), username, 10);
        JTextField serverTextField = new JTextField(server);
        JTextField serverPortTextField = new JTextField(Integer.toString(serverPort));
        
        // user name
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(1,1,1,5);
        optionPanel.add(new JLabel(messages.getString("Client.msg.Username"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_END;
        optionPanel.add(userTextField, c);
        
        // server name
        c.gridx--;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        optionPanel.add(new JLabel(messages.getString("Client.msg.Server"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        optionPanel.add(serverTextField, c);
        
        // server port
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        optionPanel.add(new JLabel(messages.getString("Client.msg.Port"), SwingConstants.RIGHT), c);
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
        userTextField.requestFocusInWindow();
        int returnVal = JOptionPane.showConfirmDialog(this, optionPanel, 
                messages.getString("Client.msg.EnterInformation"), JOptionPane.OK_CANCEL_OPTION);
        if (returnVal != JOptionPane.OK_OPTION) {
            return;
        }
        username = userTextField.getText().trim();
        server = serverTextField.getText().trim();
        try {
            serverPort = Integer.parseInt(serverPortTextField.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, messages.getString("Client.msg.InvalidPort"));
            return;
        }
        
        if (username == null || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, messages.getString("Client.msg.InvalidInputs"));
            return;
        }
        connection = new ServerConnection();
        connection.addDataListener(this);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                String error = null;
                try {
                    connection.setUsername(username);
                    connection.connectToServer(server, serverPort);
                } catch (UnknownHostException e) {
                    error = messages.getString("Client.msg.ConnectionFailedBecauseOfUnknownServerNamePleaseCheckTheServerName");
                } catch (ConnectException e) {
                    error = 
                        messages.getString("Client.msg.ConnectionfailedPleaseCheckThatTheServerIsRunningAndThatTheClientIsConfiguredProperly")
                            + "\n" +
                        messages.getString("Client.msg.ErrorMessag") + e.getMessage();
                } catch (Exception e) {
                    error = messages.getString("Client.msg.UnknownErrorTryingToConnectToServerSeeLog");
                    logger.log(Level.SEVERE, messages.getString("Client.msg.UnknownErrorPleaseReportThisIssue"), e);
                }
                return error;
            }
            
            @Override
            protected void done() {
                try {
                    String error = get();
                    if (error != null) {
                        statusPane.showMessage(error, null);
                        JOptionPane.showMessageDialog(Client.this, messages.getString("Client.msg.CouldNotConnectSeeStatusPaneForReason"));
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    logger.log(Level.SEVERE, "Error getting connection result", ex);
                } 
            }
        };
        worker.execute();
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
        setTitle(messages.getString("Client.title.PortOChatDisconnected"));
        contactListModel.clear();
        channelListModel.clear();
        showDisconnectMessage();
        notificationTimer.stop();
        timerListener.reset();
    }
    
    private void showDisconnectMessage() {
        Collection<ChatPane> chatPanes = chatPaneMap.values();
        for (ChatPane pane : chatPanes) {
            pane.showInfoMessage(messages.getString("Client.msg.DisconnectedFromServer"), "disconnect");
        }
        
        Collection<ChatPane> channelPanes = channelPaneMap.values();
        for (ChatPane pane : channelPanes) {
            pane.showInfoMessage(messages.getString("Client.msg.DisconnectedFromServer"), "disconnect");
        }
        statusPane.showMessage(messages.getString("Client.msg.DisconnectedFromServer"), "disconnect");
    }
    
    private ServerLauncherGUI serverLauncher;
    private void startServer() {
        if (serverLauncher == null) {
            serverLauncher = ServerLauncherGUI.getServerLauncherGUI();
        }
        serverLauncher.setVisible(true);
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
                    setTitle(messages.getString("Client.title.PortOChatConnectedAs") + myUserName);
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
                        "\"" + username + "\"" + messages.getString("Client.msg.AlreadyInUseEnterAnotherName"), 
                        messages.getString("Client.title.ChooseAnotherName"), JOptionPane.ERROR_MESSAGE);
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
        SwingUtilities.invokeLater(() -> {
            if (!contactListModel.contains(user.getName())) {
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
                if (contactListModel.contains(user.getName())) {
                    contactListModel.removeElement(user.getName());
                }
                
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
        SwingUtilities.invokeLater(() -> {
            if (!channelListModel.contains(channel))
                channelListModel.addElement(channel);
        });
    }
    
    private void removeChannelFromList(final String channel) {
        SwingUtilities.invokeLater(() -> {
            channelListModel.removeElement(channel);
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
                SwingUtilities.invokeLater(() ->
                    ((ChatPane)pane).setFocus()
                );
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                setTabColor(tabIndex, color);
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

        assert SwingUtilities.isEventDispatchThread(): messages.getString("Client.msg.CreateChatPaneCalledOutsideEDT");
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
    
        if (channel == null || channel.isEmpty()) {
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
    public void userAdded(User user) {
        addUser(user);
    }

    @Override
    public void userRemoved(User user) {
        removeUser(user);
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
                logger.warning(messages.getString("Client.msg.ReceivedAMessageFromAChannelThatIsNotJoined"));
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
    public void receiveChannelJoinPart(String userId, String channel, boolean join) {
        User user = serverData.getUserById(userId);
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
    public void userDoesNotExist(String username) {
        ChatPane pane = chatPaneMap.get(username);
        if (pane != null) {
            pane.showInfoMessage(username + " is not connected", "disconnect");
        }
    }
    
    @Override
    public boolean sendMessage(String recipient, boolean isChannel, boolean action, String message) {
        boolean sent = true;
        if (connection != null) {
            if (isChannel) {
                connection.sendMessage(recipient, isChannel, action, message);
            } else {
                User user = serverData.getUserByName(recipient);
                if (user != null) {
                    connection.sendMessage(user.getId(), isChannel, action, message);
                } else {
                    JOptionPane.showMessageDialog(this, recipient + " not found in user list", "No such user", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            sent = false;
        }
        return sent;
    }

    @Override
    public String getConnectedUsername() {
        return myUserName;
    }
    
    private class NotificationTimerListener implements ActionListener, 
            WindowFocusListener {
        
        private String currentTitle;
        private final String dialogTitle = messages.getString("client.title.DialogTitle");
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
                        setTitle(messages.getString("Client.title.MessageReceived") + currentTitle);
                    } else {
                        setTitle(currentTitle);
                    }
                } else {
                    // split view, update dialog instead
                    if (chatContainerDialog.getTitle().equals(dialogTitle)) {
                        chatContainerDialog.setTitle(messages.getString("Client.title.splitview.MessageReceived"));
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
