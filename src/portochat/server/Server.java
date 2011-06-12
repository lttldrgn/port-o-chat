/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.protocol.ChatMessage;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Ping;
import portochat.common.protocol.Pong;
import portochat.common.protocol.ServerMessage;
import portochat.common.protocol.UserData;
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

    public Server() {
        userDatabase = UserDatabase.getInstance();
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
            Socket socket = (Socket)event.getSource();
            DefaultData defaultData = event.getData();
            
            String user = userDatabase.getSocketUser(socket);
            if (user == null) {
                // Users must send a UserData packet first
                if (defaultData instanceof UserData) {
                    // Add
                    UserData userData = (UserData)defaultData;
                    boolean success = userDatabase.addUser(userData.getUser(), socket);
                    
                    ServerMessage serverMessage = new ServerMessage();
                    serverMessage.setMessage("Set user to: " + userData.getUser());
                    tcpSocket.writeData(socket, serverMessage);
                    // TODO username in use
                } else {
                    ServerMessage serverMessage = new ServerMessage();
                    serverMessage.setMessage("You must first send a username!");
                    tcpSocket.writeData(socket, serverMessage);
                }
            } else if (defaultData instanceof UserData) {
                // Rename
                UserData userData = (UserData)defaultData;
                boolean success = userDatabase.renameUser(user, userData.getUser(), socket);
                // TODO username in use
            } else if (defaultData instanceof Ping) {
                // Send a pong
                Pong pong = new Pong();
                pong.setTimestamp(((Ping)defaultData).getTimestamp());
                tcpSocket.writeData(socket, pong);
            } else if (defaultData instanceof ChatMessage) {
                ChatMessage chatMessage = ((ChatMessage)defaultData);
                // Fill out who sent it
                chatMessage.setFromUser(user);
                Socket toUserSocket = userDatabase.getUserSocket(chatMessage.getToUser());
                if (toUserSocket != null) {
                    tcpSocket.writeData(toUserSocket, chatMessage);
                } else {
                    ServerMessage serverMessage = new ServerMessage();
                    serverMessage.setMessage("No such Nick/Channel: " + chatMessage.getToUser());
                    tcpSocket.writeData(socket, serverMessage);
                }
            }
        }
    }
}
