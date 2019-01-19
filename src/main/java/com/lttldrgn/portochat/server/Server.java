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
import com.lttldrgn.portochat.common.protocol.DefaultData;
import com.lttldrgn.portochat.common.protocol.ServerMessage;
import com.lttldrgn.portochat.common.protocol.ServerMessageEnum;
import com.lttldrgn.portochat.common.network.event.NetEvent;
import com.lttldrgn.portochat.common.network.event.NetListener;
import com.lttldrgn.portochat.common.protocol.ProtoMessage;
import com.lttldrgn.portochat.common.protocol.ProtoUtil;
import com.lttldrgn.portochat.common.protocol.UserDoesNotExist;
import com.lttldrgn.portochat.proto.Portochat;
import com.lttldrgn.portochat.proto.Portochat.ChannelPart;
import com.lttldrgn.portochat.proto.Portochat.ChatMessage;
import com.lttldrgn.portochat.proto.Portochat.Notification;
import com.lttldrgn.portochat.proto.Portochat.PortoChatMessage;
import com.lttldrgn.portochat.proto.Portochat.Request;
import com.lttldrgn.portochat.proto.Portochat.Response;
import com.lttldrgn.portochat.proto.Portochat.UserConnectionStatus;
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

            if (defaultData instanceof ProtoMessage) {
                handleProtoMessage((ProtoMessage)defaultData, user, socket);
                
            } else {
                logger.log(Level.WARNING, "Unhandled message: {0}", defaultData);
            }
        }
    }

    private void handleProtoMessage(ProtoMessage protoMessage, User user, Socket socket) {
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
            case RESPONSE:
                Response response = protoMessage.getMessage().getResponse();
                if (response != null) {
                    handleResponse(response, user, socket);
                }
                break;
            case PING:
                // Send a pong
                long time = protoMessage.getMessage().getPing().getTimestamp();
                ProtoMessage pongMessage = new ProtoMessage(ProtoUtil.createPong(time));
                connection.writeData(socket, pongMessage);
                break;
            case PONG:
                logger.log(Level.INFO, "Updating {0} last seen", user);
                user.setLastSeen(System.currentTimeMillis());
                break;
            case CHATMESSAGE:
                handleChatMessage(protoMessage.getMessage().getChatMessage(), socket, user);
                break;
            default:
                logger.log(Level.INFO, "Message type not supported: {0}", protoMessage.getMessage().getApplicationMessageCase());
        }
    }
    private void handleRequest(User user, Request request, Socket socket) {
        switch (request.getRequestType()) {
            case ChannelJoin:
                handleChannelJoinRequest(user, request);
                break;
            case ChannelList:
            {
                PortoChatMessage channelList = ProtoUtil.createChannelList(channelDatabase.getListOfChannels());
                ProtoMessage protoMessage = new ProtoMessage(channelList);
                connection.writeData(socket, protoMessage);
            }
                break;
            case ChannelUserList:
            {
                String channel = request.getStringRequestData().getValue();
                PortoChatMessage channelUserList = ProtoUtil.createUserList(channelDatabase.getUsersInChannel(channel), channel);
                ProtoMessage protoMessage = new ProtoMessage(channelUserList);
                connection.writeData(socket, protoMessage);
            }
                break;
            case SetUserName:
                handleSetUserNameRequest(user, request.getStringRequestData().getValue(), socket);
                break;
            case SetUserPublicKey:
                handleSetUserPublicKey(user, request, socket);
            case UserList:
            {
                PortoChatMessage userList = ProtoUtil.createUserList(userDatabase.getUserList(), null);
                ProtoMessage protoMessage = new ProtoMessage(userList);
                connection.writeData(socket, protoMessage);
            }
                break;
            default:
                logger.log(Level.INFO, "Unhandled request type: {0}", request.getRequestType());
                break;
        }
    }

    private void handleResponse(Response response, User user, Socket socket) {
        switch (response.getResponseType()) {
            case ServerKeyAccepted:
                userDatabase.setSocketIsEncrypted(socket, true);
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
                ProtoMessage userConnection = new ProtoMessage(ProtoUtil.createUserConnectionStatus(user, true));

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

    private void handleSetUserPublicKey(User user, Request request, Socket socket) {

        user.setSecretKey(encryptionManager.generateServerSecretKey());
        user.setClientPublicKey(encryptionManager.getClientPublicKey(request.getByteData().toByteArray()));
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "public: {0}",
                    Util.byteArrayToHexString(user.getClientPublicKey().getEncoded()));
        }

        // Encode the private key using the client's public key
        byte[] encodedEncryptedSecretKey
                = encryptionManager.encryptSecretKeyWithPublicKey(
                        user.getSecretKey(), user.getClientPublicKey());
        ProtoMessage protoMessage = new ProtoMessage(ProtoUtil.createSetServerSharedKey(encodedEncryptedSecretKey));
        connection.writeData(socket, protoMessage);
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
        Portochat.PortoChatMessage join = ProtoUtil.createChannelJoinNotification(channel, user.getName());
        ProtoMessage protoMessage = new ProtoMessage(join);
        sendToChannelUsers(channel, user, protoMessage);
    }

    private void handleNotification(User user, Notification notification, Socket socket) {
        switch (notification.getNotificationDataCase()) {
            case CHANNELPART:
                handleChannelPartNotification(user, notification.getChannelPart());
                break;
            case USERCONNECTIONSTATUS:
                handleUserConnectionStatus(notification.getUserConnectionStatus());
                break;
            default:
                logger.log(Level.INFO, "Unsupported notification type: {0}", notification.getNotificationDataCase());
        }
    }

    private void handleChannelPartNotification(User user, ChannelPart channelPart) {
        String channel = channelPart.getChannel();
        channelDatabase.removeUserFromChannel(channel, user);

        if (!channelDatabase.channelExists(channel)) {
            // Channel was removed when user left so notify all users of removal
            notifyChannelStatusChange(channel, false);
        } else {
            // notify users in channel of part event
            Portochat.PortoChatMessage newPart =
                    ProtoUtil.createChannelPartNotification(channel, channelPart.getUserId());
            ProtoMessage protoMessage = new ProtoMessage(newPart);
            sendToChannelUsers(channel, user, protoMessage);
        }
    }

    private void handleUserConnectionStatus(UserConnectionStatus userConnection) {
        User user = ProtoUtil.convertToUser(userConnection.getUser());
        // Should be only getting disconnects here, but check anyway
        if (!userConnection.getConnected()) {
            boolean success = userDatabase.removeUser(user);

            ArrayList<String> userChannelList =
                    (ArrayList<String>) channelDatabase.getUserChannels(user);

            channelDatabase.removeUserFromAllChannels(user);

            for (String channel : userChannelList) {
                if (!channelDatabase.channelExists(channel)) {
                    notifyChannelStatusChange(channel, false);
                }
            }
        }

        ProtoMessage connectStatus = new ProtoMessage(ProtoUtil.createUserConnectionStatus(user, false));
        // Send to all other clients
        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();
        sendToAllSockets(userSocketList, connectStatus);

        // Log the connection
        logger.info(userConnection.toString());
    }

    private void handleChatMessage(ChatMessage chatMessage, Socket socket, User user) {
        ProtoMessage protoMessage = new ProtoMessage(ProtoUtil.createChatMessage(
                chatMessage.getSenderId(),
                chatMessage.getDestinationId(),
                chatMessage.getIsChannel(),
                chatMessage.getMessage(),
                chatMessage.getIsAction()));
        if (chatMessage.getIsChannel()) {
            // Send to all users in channel
            ArrayList<Socket> userSocketList
                    = (ArrayList<Socket>) channelDatabase.getSocketsOfUsersInChannel(
                            chatMessage.getDestinationId(),
                            user);

            if (userSocketList != null) {
                for (Socket userSocket : userSocketList) {
                    connection.writeData(userSocket, protoMessage);
                }
            } else {
                ServerMessage serverMessage = new ServerMessage();
                serverMessage.setMessageEnum(ServerMessageEnum.ERROR_CHANNEL_NON_EXISTENT);
                serverMessage.setAdditionalMessage(chatMessage.getDestinationId());
                connection.writeData(socket, serverMessage);
            }
        } else {
            // direct user message
            Socket toUserSocket = userDatabase.getSocketByUserId(chatMessage.getDestinationId());
            if (toUserSocket != null) {
                connection.writeData(toUserSocket, protoMessage);
            } else {
                UserDoesNotExist userDoesNotExist = new UserDoesNotExist(chatMessage.getDestinationId());
                connection.writeData(socket, userDoesNotExist);
            }
        }
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
        PortoChatMessage message;
        if (created) {
            message = ProtoUtil.createChannelAddedNotification(channel);
        } else {
            message = ProtoUtil.createChannelRemovedNotification(channel);
        }
        ProtoMessage protoMessage = new ProtoMessage(message);
        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();

        sendToAllSockets(userSocketList, protoMessage);
    }
    
    private void pingAllClients() {
        // send a message to all clients to see if they are alive
        ProtoMessage pingMessage = new ProtoMessage(ProtoUtil.createPing(0));
        ArrayList<Socket> userSocketList =
                (ArrayList<Socket>) userDatabase.getSocketList();

        sendToAllSockets(userSocketList, pingMessage);
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
