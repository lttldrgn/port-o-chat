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

import com.lttldrgn.portochat.common.User;
import com.lttldrgn.portochat.common.encryption.EncryptionManager;
import com.lttldrgn.portochat.common.network.ConnectionHandler;
import com.lttldrgn.portochat.common.network.event.NetEvent;
import com.lttldrgn.portochat.common.network.event.NetListener;
import com.lttldrgn.portochat.common.protocol.DefaultData;
import com.lttldrgn.portochat.common.protocol.ProtoMessage;
import com.lttldrgn.portochat.common.protocol.ProtoUtil;
import com.lttldrgn.portochat.common.protocol.ServerMessage;
import com.lttldrgn.portochat.common.protocol.UserDoesNotExist;
import com.lttldrgn.portochat.proto.Portochat;
import com.lttldrgn.portochat.proto.Portochat.ChannelJoin;
import com.lttldrgn.portochat.proto.Portochat.ChannelList;
import com.lttldrgn.portochat.proto.Portochat.ChannelPart;
import com.lttldrgn.portochat.proto.Portochat.ChatMessage;
import com.lttldrgn.portochat.proto.Portochat.Notification;
import com.lttldrgn.portochat.proto.Portochat.Request;
import com.lttldrgn.portochat.proto.Portochat.StringList;
import com.lttldrgn.portochat.proto.Portochat.UserList;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;

/**
 * Handles all the client interaction with the server
 * 
 */
public class ServerConnection {
    private static final Logger logger = 
            Logger.getLogger(ServerConnection.class.getName());
    private final CopyOnWriteArrayList<ServerDataListener> listeners = 
            new CopyOnWriteArrayList<>();
    private ConnectionHandler socket = null;
    private ClientHandler clientHandler = null;
    private String username = null;
    private EncryptionManager encryptionManager = null;
    
    public ServerConnection() {
        encryptionManager = EncryptionManager.getInstance();
    }
    
    public boolean connectToServer(String serverAddress, int port) 
            throws IOException {
        boolean successful;
        socket = new ConnectionHandler("Client");
        successful = socket.connect(serverAddress, port);
        
        if (successful) {
            clientHandler = new ClientHandler();
            socket.addListener(clientHandler);
            sendUserPublicKey();
        }
        return successful;
    }
    
    public void disconnect() {
        socket.disconnect();
        socket.removeListener(clientHandler);
        socket = null;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Send the user public key to the server
     */
    public void sendUserPublicKey() {
        byte encodedKey[] = encryptionManager.getClientEncodedPublicKey();
        if (encodedKey != null) {
            ProtoMessage protoMessage = new ProtoMessage(ProtoUtil.createSetPublicKey(encodedKey));
            socket.writeData(protoMessage);
        }
        
    }
    
    public void sendUsername(String newUsername) {
        Portochat.PortoChatMessage request = ProtoUtil.createSetUserNameRequest(newUsername);
        ProtoMessage message = new ProtoMessage(request);
        socket.writeData(message);
    }
    
    public void sendPing() {
        ProtoMessage pingMessage = new ProtoMessage(ProtoUtil.createPing(System.currentTimeMillis()));
        socket.writeData(pingMessage);
    }
    
    /**
     * Sends a message to the defined recipient
     * @param recipientId Person or channel the message is being sent to
     * @param isChannel Parameter should be true if the recipient is a channel
     * @param action True if this is an action message
     * @param message Message being sent
     */
    public void sendMessage(String recipientId, boolean isChannel, boolean action, String message) {
        // TODO use a real ID instead of the username
        ProtoMessage chatMessage = new ProtoMessage(ProtoUtil.createChatMessage(username, recipientId, isChannel, message, action));
        socket.writeData(chatMessage);
    }
    
    public void sendUserListRequest() {
        Portochat.PortoChatMessage message = ProtoUtil.createUserListRequest();
        ProtoMessage protoMessage = new ProtoMessage(message);
        socket.writeData(protoMessage);
    }
    
    public void joinChannel(String channel) {
        Portochat.PortoChatMessage request = ProtoUtil.createChannelJoinRequest(channel);
        ProtoMessage protoMessage = new ProtoMessage(request);
        socket.writeData(protoMessage);
    }
    
    public void partChannel(String channel) {
        Portochat.PortoChatMessage notification = ProtoUtil.createChannelPartNotification(channel, username);
        ProtoMessage protoMessage = new ProtoMessage(notification);
        socket.writeData(protoMessage);
    }
    
    public void requestListOfChannels() {
        Portochat.PortoChatMessage message = ProtoUtil.createChannelListRequest();
        ProtoMessage protoMessage = new ProtoMessage(message);
        socket.writeData(protoMessage);
    }
    
    public void requestUsersInChannel(String channel) {
        Portochat.PortoChatMessage message = ProtoUtil.createChannelUserListRequest(channel);
        ProtoMessage protoMessage = new ProtoMessage(message);
        socket.writeData(protoMessage);
    }

    public void addDataListener(ServerDataListener listener) {
        listeners.add(listener);
    }
    
    public void removeDataListener(ServerDataListener listener) {
        listeners.remove(listener);
    }
    
     private class ClientHandler implements NetListener {

        @Override
        public void incomingMessage(NetEvent event) {
            ResourceBundle messages = ResourceBundle.getBundle("portochat/resource/MessagesBundle", java.util.Locale.getDefault());
            DefaultData defaultData = event.getData();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(defaultData.toString());
            }
            
            if (defaultData instanceof ServerMessage) {
                ServerMessage message = (ServerMessage) defaultData;
                switch (message.getMessageEnum()) {
                    case USERNAME_SET:
                        for (ServerDataListener listener : listeners) {
                            listener.handleServerConnection(message.getAdditionalMessage(), true);
                        }
                        break;
                        
                    case ERROR_USERNAME_IN_USE:
                        for (ServerDataListener listener : listeners) {
                            listener.handleServerConnection(message.getAdditionalMessage(), false);
                        }
                        break;
                }
            } else if (defaultData instanceof ProtoMessage) {
                ProtoMessage protoMessage = (ProtoMessage) defaultData;
                switch (protoMessage.getMessage().getApplicationMessageCase()) {
                    case CHANNELLIST:
                        handleChannelList(protoMessage.getMessage().getChannelList());
                        break;
                    case CHATMESSAGE:
                        handleChatMessage(protoMessage.getMessage().getChatMessage());
                        break;
                    case NOTIFICATION:
                        handleNotification(protoMessage.getMessage().getNotification());
                        break;
                    case REQUEST:
                        handleRequest(protoMessage.getMessage().getRequest());
                        break;
                    case USERLIST:
                        UserList userList = protoMessage.getMessage().getUserList();
                        List<User> users = ProtoUtil.getUserList(userList);
                        String channel = userList.getChannel();
                        if (channel == null || channel.isEmpty()) {
                            ServerDataStorage.getInstance().addUsers(users);
                        }
                        for (ServerDataListener listener : listeners) {
                            listener.userListReceived(users, channel);
                        }
                        break;
                    case PING:
                        // Send a pong
                        long time = protoMessage.getMessage().getPing().getTimestamp();
                        ProtoMessage pongMessage = new ProtoMessage(ProtoUtil.createPong(time));
                        socket.writeData(pongMessage);
                        break;
                    case PONG:
                        long pingTime = protoMessage.getMessage().getPong().getTimestamp();
                        System.out.println(messages.getString("ServerConnection.msg.ServerLag") +
                                (System.currentTimeMillis() - pingTime) +
                                messages.getString("ServerConnection.msg.Ms"));
                        break;
                }
            } else if (defaultData instanceof UserDoesNotExist) { 
                for (ServerDataListener listener : listeners) {
                    listener.userDoesNotExist(((UserDoesNotExist)defaultData).getUser());
                }
            } else {
                logger.log(Level.WARNING, "{0}{1}", new Object[]{messages.getString("ServerConnection.msg.UnknownMessage"), defaultData});
            }
        }

        private void handleChannelList(ChannelList channelList) {
            // Received a channel list
            StringList stringList = channelList.getChannels();
            List<String> channels = stringList.getValuesList();
            for (ServerDataListener listener : listeners) {
                listener.channelListReceived(channels);
            }
        }

        private void handleChatMessage(ChatMessage message) {
            String channel = message.getIsChannel() ? message.getDestinationId(): null;
            // TODO once sender ID is being used, look up by ID instead of name
            User sender = ServerDataStorage.getInstance().getUserByName(message.getSenderId());
            if (sender != null) {
                for (ServerDataListener listener : listeners) {
                    listener.receiveChatMessage(sender,
                            message.getIsAction(), message.getMessage(), channel);
                }
            }
        }

        private void handleNotification(Notification notification) {
            switch (notification.getNotificationDataCase()) {
                case CHANNELJOIN:
                    ChannelJoin join = notification.getChannelJoin();
                    handleChannelJoinPart(join.getUserId(), join.getChannel(), true);
                    break;
                case CHANNELPART:
                    ChannelPart part = notification.getChannelPart();
                    handleChannelJoinPart(part.getUserId(), part.getChannel(), false);
                    break;
                case CHANNELADDED:
                    String channelAdded = notification.getChannelAdded().getChannel();
                    listeners.forEach((listener) -> listener.channelStatusReceived(channelAdded, true));
                    break;
                case CHANNELREMOVED:
                    String channelRemoved = notification.getChannelRemoved().getChannel();
                    listeners.forEach((listener) -> listener.channelStatusReceived(channelRemoved, false));
                    break;
                case USERCONNECTIONSTATUS:
                    User user = ProtoUtil.convertToUser(notification.getUserConnectionStatus().getUser());
                    boolean connected = notification.getUserConnectionStatus().getConnected();
                    if (connected) {
                        ServerDataStorage.getInstance().addUser(user);
                    } else {
                        ServerDataStorage.getInstance().removeUser(user.getId());
                    }
                    break;
            }
        }

        private void handleChannelJoinPart(String userId, String channel, boolean joined) {
            for (ServerDataListener listener : listeners) {
                listener.receiveChannelJoinPart(userId, channel, joined);
            }
        }

        private void handleRequest(Request request) {
            switch (request.getRequestType()) {
                case SetServerSharedKey:
                    setServerSecretKey(request);
                    break;
            }
        }
        private void setServerSecretKey(Request request) {
             SecretKey serverSecretKey = encryptionManager.decodeSecretKeyWithPrivateKey(
                     request.getByteData().toByteArray());
             encryptionManager.setServerSecretKey(serverSecretKey);
             ProtoMessage protoMessage = new ProtoMessage(ProtoUtil.createServerKeyAccepted(request.getRequestId()));
             protoMessage.setCanBeEncrypted(false);
             socket.writeData(protoMessage);
             sendUsername(ServerConnection.this.username);
         }
    }
}
