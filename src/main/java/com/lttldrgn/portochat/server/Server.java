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
package com.lttldrgn.portochat.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.lttldrgn.portochat.common.User;
import com.lttldrgn.portochat.common.Util;
import com.lttldrgn.portochat.common.encryption.EncryptionManager;
import com.lttldrgn.portochat.common.protocol.ChannelJoinPart;
import com.lttldrgn.portochat.common.protocol.ChannelList;
import com.lttldrgn.portochat.common.protocol.ChannelStatus;
import com.lttldrgn.portochat.common.protocol.ChatMessage;
import com.lttldrgn.portochat.common.protocol.DefaultData;
import com.lttldrgn.portochat.common.protocol.Ping;
import com.lttldrgn.portochat.common.protocol.Pong;
import com.lttldrgn.portochat.common.protocol.ServerMessage;
import com.lttldrgn.portochat.common.protocol.ServerMessageEnum;
import com.lttldrgn.portochat.common.protocol.UserConnectionStatus;
import com.lttldrgn.portochat.common.protocol.UserList;
import com.lttldrgn.portochat.common.network.event.NetEvent;
import com.lttldrgn.portochat.common.network.event.NetListener;
import com.lttldrgn.portochat.common.protocol.ProtoMessage;
import com.lttldrgn.portochat.common.protocol.ServerKeyAccepted;
import com.lttldrgn.portochat.common.protocol.ServerSharedKey;
import com.lttldrgn.portochat.common.protocol.SetPublicKey;
import com.lttldrgn.portochat.common.protocol.UserDoesNotExist;
import com.lttldrgn.portochat.proto.Portochat.Notification;
import com.lttldrgn.portochat.proto.Portochat.Request;
import com.lttldrgn.portochat.server.network.ServerConnectionHandler;

/**
 * This class handles the server connection, and populating the user/channel
 * databases with the incoming messages.
 * 
 * @author Mike
 */
public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private final EncryptionManager encryptionManager;
    private final UserDatabase userDatabase;
    private final ChannelDatabase channelDatabase;
    private ServerConnectionHandler connection;
    private final Timer timer;
    private final TimerTask task;
    private final int CLIENT_POLL_INTERVAL_MILLIS = 60000;
    
    /**
     * Public constructor
     */
    public Server() {
        encryptionManager = EncryptionManager.getInstance();
        userDatabase = UserDatabase.getInstance();
        channelDatabase = ChannelDatabase.getInstance();
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    pingAllClients();
                    removeStaleClients();
                } catch (Exception e) {
                    
                }
            }
        };
        timer.schedule(task, 5000, CLIENT_POLL_INTERVAL_MILLIS);
    }

    /**
     * Binds to a port to listen on
     * 
     * @param port the port number
     * 
     * @return true if successful
     */
    public boolean bind(int port) {

        boolean success;

        try {
            connection = new ServerConnectionHandler("Server");
            success = connection.bind(port);

            if (success) {
                connection.addListener(new ServerHandler());
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

    /**
     * Shuts the server down
     */
    public void shutdown() {
        
        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();
        for (Socket socket : userSocketList) {
            try {
                // TODO: Send disconnect server message to clients
                socket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        connection.disconnect();
        // TODO: Clear out databases
    }

    /**
     * This class handles incoming messages to the server
     */
    private class ServerHandler implements NetListener {

        /**
         * This method handles the incoming events
         */
        @Override
        public void incomingMessage(NetEvent event) {
            Socket socket = (Socket) event.getSource();
            DefaultData defaultData = event.getData();

            User user = userDatabase.getUserOfSocket(socket);

            if (defaultData instanceof Ping) {
                // Send a pong
                Pong pong = new Pong();
                pong.setTimestamp(((Ping) defaultData).getTimestamp());
                connection.writeData(socket, pong);
            } else if (defaultData instanceof Pong) {
                logger.log(Level.INFO, "Updating {0} last seen", user);
                user.setLastSeen(System.currentTimeMillis());
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
                            connection.writeData(userSocket, chatMessage);
                        }
                    } else {
                        ServerMessage serverMessage = new ServerMessage();
                        serverMessage.setMessageEnum(ServerMessageEnum.ERROR_CHANNEL_NON_EXISTENT);
                        serverMessage.setAdditionalMessage(chatMessage.getTo());
                        connection.writeData(socket, serverMessage);
                    }
                } else {
                    Socket toUserSocket = userDatabase.getSocketForUser(chatMessage.getTo());
                    if (toUserSocket != null) {
                        connection.writeData(toUserSocket, chatMessage);
                    } else {
                        UserDoesNotExist userDoesNotExist = new UserDoesNotExist(chatMessage.getTo());
                        connection.writeData(socket, userDoesNotExist);
                    }
                }
            } else if (defaultData instanceof UserConnectionStatus) {
                UserConnectionStatus userConnection = ((UserConnectionStatus) defaultData);

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
            } else if (defaultData instanceof ProtoMessage) {
                ProtoMessage protoMessage = (ProtoMessage) defaultData;
                switch (protoMessage.getMessage().getApplicationMessageCase()) {
                    case NOTIFICATION:
                        Notification notification = protoMessage.getMessage().getNotification();
                        if (notification != null) {
                            handleNotification(user, notification, socket);
                        }
                        break;
                    case REQUEST:
                        Request request = protoMessage.getMessage().getRequest();
                        if (request != null) {
                            handleRequest(user, request, socket);
                        }
                        break;
                    default:
                        logger.log(Level.INFO, "Message type not supported: {0}", protoMessage.getMessage().getApplicationMessageCase());
                }
                
            } else if (defaultData instanceof SetPublicKey) {
                SetPublicKey pubKey = (SetPublicKey) defaultData;
                user.setSecretKey(encryptionManager.generateServerSecretKey());
                user.setClientPublicKey(encryptionManager.getClientPublicKey(pubKey.getEncodedPublicKey()));
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "public: {0}",
                            Util.byteArrayToHexString(user.getClientPublicKey().getEncoded()));
                }

                // Encode the private key using the client's public key
                byte[] encodedEncryptedSecretKey =
                        encryptionManager.encryptSecretKeyWithPublicKey(
                        user.getSecretKey(), user.getClientPublicKey());
                ServerSharedKey sharedKey = new ServerSharedKey();
                sharedKey.setEncryptedSecretKey(encodedEncryptedSecretKey);
                connection.writeData(socket, sharedKey);
            } else if (defaultData instanceof ServerKeyAccepted) {
                userDatabase.setSocketIsEncrypted(socket, true);
            } else {
                logger.log(Level.WARNING, "Unhandled message: {0}", defaultData);
            }
        }
    }

    private void handleRequest(User user, Request request, Socket socket) {
        switch (request.getRequestType()) {
            case ChannelJoin:
                handleChannelJoinRequest(user, request);
                break;
            case ChannelList:
                ChannelList channelList = new ChannelList();
                channelList.setChannelList(channelDatabase.getListOfChannels());
                connection.writeData(socket, channelList);
                break;
            case ChannelUserList:
                UserList channelUserList = new UserList();
                String channel = request.getStringRequestData().getValue();
                channelUserList.setChannel(channel);
                channelUserList.setUserList(channelDatabase.getUsersInChannel(channel));
                connection.writeData(socket, channelUserList);
                break;
            case SetUserName:
                handleSetUserNameRequest(user, request.getStringRequestData().getValue(), socket);
                break;
            case UserList:
                UserList userList = new UserList();
                userList.setUserList(userDatabase.getUserList());
                connection.writeData(socket, userList);
                break;
            default:
                logger.log(Level.INFO, "Unhandled request type: {0}", request.getRequestType());
                break;
        }
    }

    private void handleSetUserNameRequest(User user, String newName, Socket socket) {
        // Set user info
        String oldUserName = user.getName();
        boolean rename = (oldUserName != null);
        boolean success;

        if (!rename) {
            // First time setting a name
            success = userDatabase.addUser(newName, socket);
        } else {
            // Rename
            success = userDatabase.renameUser(user, newName);

            // Log
            logger.log(Level.INFO, "Renamed of {0} to {1} was {2}",
                    new Object[]{oldUserName, newName,
                        success ? "successful!" : "unsuccessful!"});
        }

        ServerMessage serverMessage = new ServerMessage();
        if (success) {
            user.setLastSeen(System.currentTimeMillis());
            if (!rename) {
                // Notify other users of connection
                UserConnectionStatus userConnection = new UserConnectionStatus();
                userConnection.setUser(user);
                userConnection.setConnected(true);

                ArrayList<Socket> userSocketList
                        = (ArrayList<Socket>) userDatabase.getSocketList();
                sendToAllSockets(userSocketList, userConnection);
            } else {
                // update channel database
                channelDatabase.renameUser(oldUserName, newName);
            }
            serverMessage.setMessageEnum(ServerMessageEnum.USERNAME_SET);
            serverMessage.setAdditionalMessage(newName);
        } else {
            serverMessage.setMessageEnum(ServerMessageEnum.ERROR_USERNAME_IN_USE);
            serverMessage.setAdditionalMessage(newName);
        }
        connection.writeData(socket, serverMessage);
    }

    private void sendToChannelUsers(String channel, User filterUser, DefaultData data) {
        // Notify users in channel
        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) channelDatabase.getSocketsOfUsersInChannel(
                channel,
                filterUser);
        sendToAllSockets(userSocketList, data);
    }

    private void handleChannelJoinRequest(User user, Request request) {
        String channel = request.getStringRequestData().getValue();
        if (!channelDatabase.channelExists(channel)) {
            // Creating channel, notify all users
            notifyChannelStatusChange(channel, true);
        }
        channelDatabase.addUserToChannel(channel, user);

        // notify all apps
        ChannelJoinPart join = new ChannelJoinPart();
        join.setChannel(channel);
        join.setJoined(true);
        join.setUser(user);
        sendToChannelUsers(channel, user, join);
    }

    private void handleNotification(User user, Notification notification, Socket socket) {
        switch (notification.getType()) {
            case ChannelPart:
                handleChannelPartNoticiation(user, notification.getStringData().getValue());
                break;
            default:
                logger.log(Level.INFO, "Unsupported notification type: {0}", notification.getType());
        }
    }

    private void handleChannelPartNoticiation(User user, String channel) {

        channelDatabase.removeUserFromChannel(channel, user);

        if (!channelDatabase.channelExists(channel)) {
            // Channel was removed when user left so notify all users
            notifyChannelStatusChange(channel, false);
        }

        ChannelJoinPart join = new ChannelJoinPart();
        join.setChannel(channel);
        join.setJoined(false);
        join.setUser(user);
        sendToChannelUsers(channel, user, join);
    }

    private void sendToAllSockets(List<Socket> userSocketList, DefaultData data) {
        if (userSocketList != null && userSocketList.size() > 0) {
            for (Socket userSocket : userSocketList) {
                connection.writeData(userSocket, data);
            }
        }
    }

    /**
     * Notifies of channel status change (creation/deletions)
     * 
     * @param channel
     * @param created true if created, false if deleted
     */
    private void notifyChannelStatusChange(String channel, boolean created) {
        ChannelStatus channelStatus = new ChannelStatus();
        channelStatus.setChannel(channel);
        channelStatus.setCreated(created);

        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();

        sendToAllSockets(userSocketList, channelStatus);
    }
    
    private void pingAllClients() {
        Ping ping = new Ping();
        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();

        sendToAllSockets(userSocketList, ping);
    }
    
    private final long CLIENT_TIMEOUT = 3 * 60 * 1000;
    private void removeStaleClients() {
        long now = System.currentTimeMillis();
        for (User user : userDatabase.getUserList()) {
            if ((now - user.getLastSeen()) > CLIENT_TIMEOUT) {
                logger.log(Level.INFO, "{0} timed out", user);
                // disconnect socket for user and remove
                // TODO we can clean up manually or maybe try to send a message over that socket to make it error out?
                Socket socket = userDatabase.getSocketForUser(user);
//                socket.close();
//                userDatabase.removeUser(user);
            }
        }
    }
}
