/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lttldrgn.portochat.server;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import com.lttldrgn.portochat.common.Settings;

/**
 *
 * @author Brandon
 */
public class ServerLauncherGUI extends JFrame implements ActionListener {
    private Server server;
    private JButton stopStartButton = new JButton("Start Server");
    private JTextField portEntryField = new JTextField(5);
    // TODO: Show console with server output
    private JTextArea console = new JTextArea();
    //private PrintStream out = new PrintStream();
    private boolean running = false;
    
    private ServerLauncherGUI() {
        super("Port-O-Chat Server");
        server = ServerLauncher.launchServer();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private void init() {
        setLayout(new FlowLayout());
        add(new JLabel("Port"));
        portEntryField.setText(Integer.toString(Settings.DEFAULT_SERVER_PORT));
        add(portEntryField);
        add(stopStartButton);
        stopStartButton.addActionListener(this);
        pack();
    }
    
    public static ServerLauncherGUI getServerLauncherGUI() {
        ServerLauncherGUI gui = new ServerLauncherGUI();
        gui.init();
        return gui;
    }
    
    private void start(int port) {
        server.bind(port);
    }
    
    public static void main(String args[]) {
       ServerLauncherGUI gui = getServerLauncherGUI();
       gui.setVisible(true);
       System.out.println("Server Launched");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(stopStartButton)) {
            if (running) {
                System.out.println("Shutting down server.");
                server.shutdown();
                running = false;
                stopStartButton.setText("Start Server");
            } else {
                int port = Settings.DEFAULT_SERVER_PORT;
                try {
                    port = Integer.parseInt(portEntryField.getText());
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Port is not valid");
                    return;
                }
                start(port);
                running = true;
                stopStartButton.setText("Stop Server");
            }
        }
    }
}
