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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.encryption.EncryptionManager;
import portochat.common.protocol.ChannelJoinPart;
import portochat.common.protocol.ChannelList;
import portochat.common.protocol.ChannelStatus;
import portochat.common.protocol.ChatMessage;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Initialization;
import portochat.common.protocol.InitializationEnum;
import portochat.common.protocol.Ping;
import portochat.common.protocol.Pong;
import portochat.common.protocol.ServerMessage;
import portochat.common.protocol.UserConnectionStatus;
import portochat.common.protocol.SetUsernameRequest;
import portochat.common.protocol.UserList;
import portochat.common.network.ConnectionHandler;
import portochat.common.network.event.NetEvent;
import portochat.common.network.event.NetListener;
import java.util.ResourceBundle;
import javax.crypto.SecretKey;
import portochat.common.protocol.ServerKeyAccepted;
import portochat.common.protocol.ServerSharedKey;
import portochat.common.protocol.SetPublicKey;
import portochat.common.protocol.UserDoesNotExist;
import portochat.common.protocol.request.ChannelListRequest;
import portochat.common.protocol.request.ChannelUserListRequest;
import portochat.common.protocol.request.UserListRequest;

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
    public void sendInitialize() {
        Initialization initialization = new Initialization();
        initialization.setInitializationEnum(InitializationEnum.CLIENT_RSA_PRIVATE_KEY);
        initialization.setServer(false);
        initialization.setInitializationEnum(
                    InitializationEnum.CLIENT_RSA_PRIVATE_KEY);
        initialization.setEncodedPublicKey(
                encryptionManager.getClientEncodedPublicKey());
        
        socket.writeData(initialization);
    }
    
    public void sendUsername(String newUsername) {
        SetUsernameRequest request = new SetUsernameRequest();
        request.setUser(newUsername);
        socket.writeData(request);
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
        UserListRequest request = new UserListRequest();
        socket.writeData(request);
    }
    
    public void joinChannel(String channel) {
        ChannelJoinPart channelJoinPart = new ChannelJoinPart();
        channelJoinPart.setChannel(channel);
        channelJoinPart.setJoined(true);
        socket.writeData(channelJoinPart);
    }
    
    public void partChannel(String channel) {
        ChannelJoinPart channelJoinPart = new ChannelJoinPart();
        channelJoinPart.setChannel(channel);
        channelJoinPart.setJoined(false);
        socket.writeData(channelJoinPart);
    }
    
    public void requestListOfChannels() {
        ChannelListRequest request = new ChannelListRequest();
        socket.writeData(request);
    }
    
    public void requestUsersInChannel(String channel) {
        ChannelUserListRequest request = new ChannelUserListRequest(channel);
        socket.writeData(request);        
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
            } else if (defaultData instanceof ChannelJoinPart) {
                ChannelJoinPart joinPart = (ChannelJoinPart) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.receiveChannelJoinPart(joinPart.getUser(),
                            joinPart.getChannel(), joinPart.hasJoined());
                }
            } else if (defaultData instanceof ChannelStatus) {
                ChannelStatus status = (ChannelStatus) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.channelStatusReceived(status.getChannel(), 
                            status.isCreated());
                }
            } else if (defaultData instanceof Initialization) {
                Initialization init = (Initialization) defaultData;
                if (init.getInitializationEnum() == InitializationEnum.READY) {
                    // Send username
                    sendUsername(ServerConnection.this.username);
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
            } else {
                logger.log(Level.WARNING, "{0}{1}", new Object[]{messages.getString("ServerConnection.msg.UnknownMessage"), defaultData});
            }
        }
    }
    
}
