/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

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
    
    public ServerConnection() {
    }
    
    public boolean connectToServer(String serverAddress, int port) {
        boolean successful = true;
        socket = new TCPSocket("Client");
        successful = socket.connect(serverAddress, port);
        if (successful) {
            socket.addListener(new ClientHandler());
        }
        return successful;
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
    
    public void sendMessage(String username, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setTo(username);
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
    
    public void requsetListOfChannels() {
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
    
     private class ClientHandler implements NetListener {

        @Override
        public void incomingMessage(NetEvent event) {
            DefaultData defaultData = event.getData();
            
            if (defaultData instanceof Pong) {
                System.out.println("Server lag: " + 
                        ((Pong)defaultData).getCalculatedLag() + "ms");
            } else if (defaultData instanceof ServerMessage) {
                System.out.println(((ServerMessage)defaultData));
            } else if (defaultData instanceof ChatMessage) {
                ChatMessage message = (ChatMessage)defaultData;
                String channel = message.isChannel() ? message.getTo() : null;
                for (ServerDataListener listener : listeners) {
                    listener.receiveChatMessage(message.getFromUser(), 
                            message.getMessage(), channel);
                }
                System.out.println(((ChatMessage)defaultData));
            } else if (defaultData instanceof UserList) {
                UserList userList = (UserList) defaultData;
                String channel = userList.getChannel();
                for (ServerDataListener listener : listeners) {
                    listener.userListReceived(userList.getUserList(), channel);
                }
                System.out.println((UserList)defaultData);
            } else if (defaultData instanceof UserConnection) {
                // if user is null it's the server disconnecting
                UserConnection user = (UserConnection) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.userConnectionEvent(user.getUser(), user.isConnected());
                }
                System.out.println((UserConnection)defaultData);
            } else if (defaultData instanceof ChannelList) {
                // Received a channel list
                ChannelList channelList = (ChannelList) defaultData;
                List<String> channels = channelList.getChannelList();
                for (ServerDataListener listener : listeners) {
                    listener.channelListReceived(channels);
                }
                System.out.println(((ChannelList)defaultData));
            } else if (defaultData instanceof ChannelJoinPart) {
                ChannelJoinPart joinPart = (ChannelJoinPart) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.receiveChannelJoinPart(joinPart.getUser(), 
                            joinPart.getChannel(), joinPart.hasJoined());
                }
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(joinPart.toString());
                }
            } else if (defaultData instanceof ChannelStatus) {
                System.out.println((ChannelStatus)defaultData);
            } else {
                System.out.println("Unknown message: " + defaultData);
            }
        }
    }
    
}