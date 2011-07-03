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
import portochat.common.protocol.ChannelJoinPart;
import portochat.common.protocol.ChannelList;
import portochat.common.protocol.ChannelStatus;
import portochat.common.protocol.ChatMessage;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Ping;
import portochat.common.protocol.Pong;
import portochat.common.protocol.ServerMessage;
import portochat.common.protocol.ServerMessageEnum;
import portochat.common.protocol.UserConnection;
import portochat.common.protocol.UserData;
import portochat.common.protocol.UserList;
import portochat.common.socket.TCPSocket;
import portochat.common.socket.event.NetEvent;
import portochat.common.socket.event.NetListener;

/**
 * Handles all the client interaction with the server
 * 
 */
public class ServerConnection {
    private static final Logger logger = 
            Logger.getLogger(ServerConnection.class.getName());
    private CopyOnWriteArrayList<ServerDataListener> listeners = 
            new CopyOnWriteArrayList<ServerDataListener>();
    private TCPSocket socket = null;
    private ClientHandler clientHandler = null;
    
    public ServerConnection() {
    }
    
    public boolean connectToServer(String serverAddress, int port) 
            throws IOException {
        boolean successful = true;
        socket = new TCPSocket("Client");
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
    
    public void sendUsername(String username) {
        UserData userData = new UserData();
        userData.setUser(username);
        socket.writeData(socket.getClientSocket(), userData);
    }
    
    public void sendPing() {
        Ping ping = new Ping();
        socket.writeData(socket.getClientSocket(), ping);
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
        socket.writeData(socket.getClientSocket(), chatMessage);
    }
    
    public void sendUserListRequest() {
        UserList userList = new UserList();
        socket.writeData(socket.getClientSocket(), userList);
    }
    
    public void joinChannel(String channel) {
        ChannelJoinPart channelJoinPart = new ChannelJoinPart();
        channelJoinPart.setChannel(channel);
        channelJoinPart.setJoined(true);
        socket.writeData(socket.getClientSocket(), channelJoinPart);
    }
    
    public void partChannel(String channel) {
        ChannelJoinPart channelJoinPart = new ChannelJoinPart();
        channelJoinPart.setChannel(channel);
        channelJoinPart.setJoined(false);
        socket.writeData(socket.getClientSocket(), channelJoinPart);
    }
    
    public void requestListOfChannels() {
        ChannelList channelList = new ChannelList();
        socket.writeData(socket.getClientSocket(), channelList);
    }
    
    public void requestUsersInChannel(String channel) {
        UserList userList = new UserList();
        userList.setChannel(channel);
        socket.writeData(socket.getClientSocket(), userList);        
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
            DefaultData defaultData = event.getData();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(defaultData.toString());
            }
            
            if (defaultData instanceof Pong) {
                System.out.println("Server lag: " + 
                        ((Pong)defaultData).getCalculatedLag() + "ms");
            } else if (defaultData instanceof ServerMessage) {
                ServerMessage message = (ServerMessage) defaultData;
                if (message.getMessageEnum().equals(ServerMessageEnum.USERNAME_SET)) {
                    for (ServerDataListener listener : listeners) {
                        listener.handleServerConnection(message.getAdditionalMessage(), true);
                    }
                } else if (message.getMessageEnum().equals(ServerMessageEnum.ERROR_USERNAME_IN_USE)) {
                    for (ServerDataListener listener : listeners) {
                        listener.handleServerConnection(message.getAdditionalMessage(), false);
                    }
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
            } else if (defaultData instanceof UserConnection) {
                // if user is null it's the server disconnecting
                UserConnection user = (UserConnection) defaultData;
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
            } else {
                logger.warning("Unknown message: " + defaultData);
            }
        }
    }
    
}