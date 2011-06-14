/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.protocol.ChannelJoinPart;
import portochat.common.protocol.ChannelList;
import portochat.common.protocol.ChannelStatus;
import portochat.common.protocol.ChatMessage;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Ping;
import portochat.common.protocol.Pong;
import portochat.common.protocol.ServerMessage;
import portochat.common.protocol.UserConnection;
import portochat.common.protocol.UserData;
import portochat.common.protocol.UserList;
import portochat.common.socket.TCPSocket;
import portochat.common.socket.event.NetEvent;
import portochat.common.socket.event.NetListener;

/**
 *
 * @author Mike
 */
public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private TCPSocket tcpSocket = null;
    private UserDatabase userDatabase = null;
    private ChannelDatabase channelDatabase = null;

    public Server() {
        userDatabase = UserDatabase.getInstance();
        channelDatabase = ChannelDatabase.getInstance();
    }

    public boolean bind(int port) {

        boolean success = true;

        try {
            tcpSocket = new TCPSocket("Server");
            success = tcpSocket.bind(port);

            if (success) {
                tcpSocket.addListener(new ServerHandler());
                logger.log(Level.INFO, "Server bound to port: {0}", port);
            } else {
                logger.log(Level.SEVERE, "Server unable to bind to port: {0}", port);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't listen on port: " + port, ex);
            success = false;
        }

        return success;
    }

    public void shutdown() {
    }

    private class ServerHandler implements NetListener {

        @Override
        public void incomingMessage(NetEvent event) {
            Socket socket = (Socket) event.getSource();
            DefaultData defaultData = event.getData();

            String user = userDatabase.getSocketOfUser(socket);
            if (user == null) {
                // Users must send a UserData packet first
                if (defaultData instanceof UserData) {
                    // Add
                    UserData userData = (UserData) defaultData;
                    boolean success = userDatabase.addUser(userData.getUser(), socket);

                    ServerMessage serverMessage = new ServerMessage();
                    if (success) {
                        serverMessage.setMessage("Set user to: " + userData.getUser());
                    } else {
                        serverMessage.setMessage("Username in use: " + userData.getUser());
                    }
                    tcpSocket.writeData(socket, serverMessage);

                    if (success) {
                        // Notify other users of connection
                        UserConnection userConnection = new UserConnection();
                        userConnection.setUser(userData.getUser());
                        userConnection.setConnected(true);

                        ArrayList<Socket> userSocketList =
                                (ArrayList<Socket>) userDatabase.getSocketList();
                        sendToAllSockets(userSocketList, userConnection);
                    }
                } else if (defaultData instanceof UserConnection) {
                    // Log the connection
                    logger.info(((UserConnection) defaultData).toString());
                } else {
                    ServerMessage serverMessage = new ServerMessage();
                    serverMessage.setMessage("You must first send a username!");
                    tcpSocket.writeData(socket, serverMessage);
                }
            } else if (defaultData instanceof UserData) {
                // Rename
                UserData userData = (UserData) defaultData;
                boolean success = userDatabase.renameUser(user, userData.getUser(), socket);
                // TODO username in use
            } else if (defaultData instanceof Ping) {
                // Send a pong
                Pong pong = new Pong();
                pong.setTimestamp(((Ping) defaultData).getTimestamp());
                tcpSocket.writeData(socket, pong);
            } else if (defaultData instanceof ChatMessage) {
                ChatMessage chatMessage = ((ChatMessage) defaultData);

                // Fill out who sent it
                chatMessage.setFromUser(user);

                if (chatMessage.isChannel()) {
                    // Send to all users in channel
                    ArrayList<Socket> userSocketList =
                            (ArrayList<Socket>) channelDatabase.getSocketsOfUsersInChannel(
                            chatMessage.getTo(),
                            chatMessage.getFromUser());

                    if (userSocketList != null) {
                        for (Socket userSocket : userSocketList) {
                            tcpSocket.writeData(userSocket, chatMessage);
                        }
                    } else {
                        ServerMessage serverMessage = new ServerMessage();
                        serverMessage.setMessage("Can't send message to a "
                                + "non-existant channel: " + chatMessage.getTo());
                        tcpSocket.writeData(socket, serverMessage);
                    }
                } else {
                    Socket toUserSocket = userDatabase.getUserOfSocket(chatMessage.getTo());
                    if (toUserSocket != null) {
                        tcpSocket.writeData(toUserSocket, chatMessage);
                    } else {
                        ServerMessage serverMessage = new ServerMessage();
                        serverMessage.setMessage("Can't send message to a "
                                + "non-existant user: " + chatMessage.getTo());
                        tcpSocket.writeData(socket, serverMessage);
                    }
                }
            } else if (defaultData instanceof UserList) {
                UserList userList = ((UserList) defaultData);
                // Fill out the user list
                String channel = userList.getChannel();
                if (channel != null) {
                    userList.setUserList(channelDatabase.getUsersInChannel(channel));
                } else {
                    userList.setUserList(userDatabase.getUserList());
                }
                tcpSocket.writeData(socket, userList);
            } else if (defaultData instanceof UserConnection) {
                UserConnection userConnection = ((UserConnection) defaultData);

                // Shouldn't have connects here anyways
                if (!userConnection.isConnected()) {
                    boolean success =
                            userDatabase.removeUser(userConnection.getUser());

                    ArrayList<String> userChannelList =
                            (ArrayList<String>) channelDatabase.getUserChannels(
                            userConnection.getUser());

                    channelDatabase.removeUserFromAllChannels(userConnection.getUser());

                    for (String channel : userChannelList) {
                        if (!channelDatabase.channelExists(channel)) {
                            notifyChannelStatusChange(channel, false);
                        }
                    }
                }

                // Send to all other clients
                ArrayList<Socket> userSocketList =
                        (ArrayList<Socket>) userDatabase.getSocketList();
                sendToAllSockets(userSocketList, userConnection);

                // Log the connection
                logger.info(userConnection.toString());
            } else if (defaultData instanceof ChannelList) {
                ChannelList channelList = ((ChannelList) defaultData);
                channelList.setChannelList(channelDatabase.getListOfChannels());
                tcpSocket.writeData(socket, channelList);
            } else if (defaultData instanceof ChannelJoinPart) {
                ChannelJoinPart channelJoinPart = ((ChannelJoinPart) defaultData);

                // Fill out the user
                channelJoinPart.setUser(user);

                if (channelJoinPart.hasJoined()) {
                    // joining
                    if (!channelDatabase.channelExists(
                            channelJoinPart.getChannel())) {
                        // Creating channel, notify all users
                        notifyChannelStatusChange(channelJoinPart.getChannel(), true);
                    }
                    channelDatabase.addUserToChannel(
                            channelJoinPart.getChannel(),
                            channelJoinPart.getUser());


                } else {
                    // leaving
                    channelDatabase.removeUserFromChannel(
                            channelJoinPart.getChannel(),
                            channelJoinPart.getUser());

                    if (!channelDatabase.channelExists(
                            channelJoinPart.getChannel())) {
                        // Removing channel, notify all users
                        notifyChannelStatusChange(channelJoinPart.getChannel(), false);
                    }
                }

                // Notify users in that channel
                ArrayList<Socket> userSocketList =
                        (ArrayList<Socket>) channelDatabase.getSocketsOfUsersInChannel(
                        channelJoinPart.getChannel(),
                        channelJoinPart.getUser());

                sendToAllSockets(userSocketList, channelJoinPart);

                // Log the join/part
                logger.info(channelJoinPart.toString());
            } else {
                logger.log(Level.WARNING, "Unhandled message: {0}", defaultData);
            }
        }
    }

    private void sendToAllSockets(List<Socket> userSocketList, DefaultData data) {
        if (userSocketList != null && userSocketList.size() > 0) {
            for (Socket userSocket : userSocketList) {
                tcpSocket.writeData(userSocket, data);
            }
        }
    }

    private void notifyChannelStatusChange(String channel, boolean created) {
        ChannelStatus channelStatus = new ChannelStatus();
        channelStatus.setChannel(channel);
        channelStatus.setCreated(created);

        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();

        sendToAllSockets(userSocketList, channelStatus);
    }
}
