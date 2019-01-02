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

import com.lttldrgn.portochat.common.encryption.EncryptionManager;
import com.lttldrgn.portochat.common.network.ConnectionHandler;
import com.lttldrgn.portochat.common.network.event.NetEvent;
import com.lttldrgn.portochat.common.network.event.NetListener;
import com.lttldrgn.portochat.common.protocol.ChannelList;
import com.lttldrgn.portochat.common.protocol.ChannelStatus;
import com.lttldrgn.portochat.common.protocol.ChatMessage;
import com.lttldrgn.portochat.common.protocol.DefaultData;
import com.lttldrgn.portochat.common.protocol.Ping;
import com.lttldrgn.portochat.common.protocol.Pong;
import com.lttldrgn.portochat.common.protocol.ProtoMessage;
import com.lttldrgn.portochat.common.protocol.ProtoUtil;
import com.lttldrgn.portochat.common.protocol.ServerKeyAccepted;
import com.lttldrgn.portochat.common.protocol.ServerMessage;
import com.lttldrgn.portochat.common.protocol.ServerSharedKey;
import com.lttldrgn.portochat.common.protocol.SetPublicKey;
import com.lttldrgn.portochat.common.protocol.UserConnectionStatus;
import com.lttldrgn.portochat.common.protocol.UserDoesNotExist;
import com.lttldrgn.portochat.common.protocol.UserList;
import com.lttldrgn.portochat.proto.Portochat;
import com.lttldrgn.portochat.proto.Portochat.ChannelJoin;
import com.lttldrgn.portochat.proto.Portochat.ChannelPart;
import com.lttldrgn.portochat.proto.Portochat.Notification;
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
        SetPublicKey setKey = new SetPublicKey();
        byte encodedKey[] = encryptionManager.getClientEncodedPublicKey();
        if (encodedKey != null) {
            setKey.setEncodedPublicKey(encodedKey);
            socket.writeData(setKey);
        }
        
    }
    
    public void sendUsername(String newUsername) {
        Portochat.PortoChatMessage request = ProtoUtil.createSetUserNameRequest(newUsername);
        ProtoMessage message = new ProtoMessage(request);
        socket.writeData(message);
    }
    
    public void sendPing() {
        Ping ping = new Ping();
        socket.writeData(ping);
    }
    
    /**
     * Sends a message to the defined recipient
     * @param recipient Person or channel the message is being sent to
     * @param action True if this is an action message
     * @param message Message being sent
     */
    public void sendMessage(String recipient, boolean action, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setTo(recipient);
        chatMessage.setAction(action);
        chatMessage.setMessage(message);
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
            
            if (defaultData instanceof Pong) {
                System.out.println(messages.getString("ServerConnection.msg.ServerLag") + 
                        ((Pong)defaultData).getCalculatedLag() + messages.getString("ServerConnection.msg.Ms"));
            } else if (defaultData instanceof ServerMessage) {
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
            } else if (defaultData instanceof ChatMessage) {
                ChatMessage message = (ChatMessage)defaultData;
                String channel = message.isChannel() ? message.getTo() : null;
                for (ServerDataListener listener : listeners) {
                    listener.receiveChatMessage(message.getFromUser(),
                            message.isAction(), message.getMessage(), channel);
                }
            } else if (defaultData instanceof UserList) {
                UserList userList = (UserList) defaultData;
                String channel = userList.getChannel();
                for (ServerDataListener listener : listeners) {
                    listener.userListReceived(userList.getUserList(), channel);
                }
            } else if (defaultData instanceof UserConnectionStatus) {
                // if user is null it's the server disconnecting
                UserConnectionStatus user = (UserConnectionStatus) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.userConnectionEvent(user.getUser(), user.isConnected());
                }
            } else if (defaultData instanceof ChannelList) {
                // Received a channel list
                ChannelList channelList = (ChannelList) defaultData;
                List<String> channels = channelList.getChannelList();
                for (ServerDataListener listener : listeners) {
                    listener.channelListReceived(channels);
                }
            } else if (defaultData instanceof ProtoMessage) {
                ProtoMessage protoMessage = (ProtoMessage) defaultData;
                switch (protoMessage.getMessage().getApplicationMessageCase()) {
                    case NOTIFICATION:
                        handleNotification(protoMessage.getMessage().getNotification());
                        break;
                }
            } else if (defaultData instanceof ChannelStatus) {
                ChannelStatus status = (ChannelStatus) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.channelStatusReceived(status.getChannel(), 
                            status.isCreated());
                }
            } else if (defaultData instanceof Ping) { 
                Pong pong = new Pong();
                pong.setTimestamp(((Ping) defaultData).getTimestamp());
                socket.writeData(pong);
            } else if (defaultData instanceof UserDoesNotExist) { 
                for (ServerDataListener listener : listeners) {
                    listener.userDoesNotExist(((UserDoesNotExist)defaultData).getUser());
                }
            } else if (defaultData instanceof ServerSharedKey) {
                ServerSharedKey sharedKey = (ServerSharedKey) defaultData;
                SecretKey serverSecretKey =
                encryptionManager.decodeSecretKeyWithPrivateKey(
                        sharedKey.getEncryptedSecretKey());
                encryptionManager.setServerSecretKey(serverSecretKey);
                ServerKeyAccepted accepted = new ServerKeyAccepted();
                socket.writeData(accepted);
                sendUsername(ServerConnection.this.username);
            } else {
                logger.log(Level.WARNING, "{0}{1}", new Object[]{messages.getString("ServerConnection.msg.UnknownMessage"), defaultData});
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
            }
        }

        private void handleChannelJoinPart(String userId, String channel, boolean joined) {
            for (ServerDataListener listener : listeners) {
                listener.receiveChannelJoinPart(userId, channel, joined);
            }
        }
    }
    
}
