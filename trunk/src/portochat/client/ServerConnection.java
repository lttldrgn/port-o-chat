/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
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
        chatMessage.setToUser(username);
        chatMessage.setMessage(message);
        socket.writeData(socket.getClientSocket(), chatMessage);
    }
    
    public void sendUserListRequest() {
        UserList userList = new UserList();
        socket.writeData(socket.getClientSocket(), userList);
    }
    
    public void joinChannel(String channel) {
        //outWriter.println(ProtocolDefinitions.JOIN_CHANNEL + channel);
    }
    
    public void leaveChannel(String channel) {
        //outWriter.println(ProtocolDefinitions.LEAVE_CHANNEL + channel);
    }
    
    public ArrayList<String> channelWho(String channel) {
        //outWriter.println(ProtocolDefinitions.LEAVE_CHANNEL + channel);
        //try {
        //    reader.readLine();
        //} catch (IOException ex) {
        //    logger.log(Level.SEVERE, null, ex);
        //}
        //return new ArrayList<String>();
        return null;
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
                System.out.println(((ChatMessage)defaultData));
            } else if (defaultData instanceof UserList) {
                UserList userList = (UserList) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.userListReceived(userList.getUserList());
                }
            } else if (defaultData instanceof UserConnection) {
                UserConnection user = (UserConnection) defaultData;
                for (ServerDataListener listener : listeners) {
                    listener.userConnectionEvent(user.getUser(), user.isConnected());
                }
                System.out.println((UserConnection)defaultData);
            } else {
                System.out.println("Unknown message: " + defaultData);
            }
        }
    }
    
}