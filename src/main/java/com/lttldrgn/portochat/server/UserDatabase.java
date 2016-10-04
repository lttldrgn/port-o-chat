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

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import com.lttldrgn.portochat.common.User;
import com.lttldrgn.portochat.common.network.handler.BufferHandler;

/**
 * This class is a singleton class used to contain the user database.
 * 
 * @author Mike
 */
public class UserDatabase {

    private static final Logger logger = Logger.getLogger(UserDatabase.class.getName());
    private static UserDatabase instance = null;
    private final Map<User, Socket> userMap = new ConcurrentHashMap<>();;
    private final Map<Socket, User> socketMap = new ConcurrentHashMap<>();;
    private final Map<Socket, Boolean> socketEncryptionMap = new ConcurrentHashMap<>();
    
    /**
     * Private constructor.
     */
    private UserDatabase() {

    }
    
    /**
     * Method to get the instance of this singleton.
     * 
     * @return UserDatabase
     */
    public synchronized static UserDatabase getInstance() {
        if (instance == null) {
            instance = new UserDatabase();
        }
        return instance;
    }
    
    public boolean addConnection(Socket socket) {
        boolean success = false;
        
        if (!socketMap.containsKey(socket)) {
            User user = new User();
            user.setHost(socket.getInetAddress().getHostName());
            socketEncryptionMap.put(socket, false); // not encrypted initially

            socketMap.put(socket, user);
            logger.log(Level.INFO, "{0} has connected", 
                    new Object[]{user});
            success = true;
        }
        
        return success;
    }
    /**
     * Adds a user to the database
     * 
     * @param userName user name to add
     * @param socket the user's socket. 
     * 
     * @return true if successful
     */
    public boolean addUser(String userName, Socket socket) {
        boolean success = false;
        
        User user = socketMap.get(socket);
        if (user != null) {
            if (!userNameInUse(userName)) {
                user.setName(userName);

                userMap.put(user, socket);
                logger.log(Level.INFO, "{0} has registered", 
                        new Object[]{user});
                success = true;
            }
        }
        
        return success;
    }
    
    /**
     * Renames a user in the database.
     * 
     * @param user User object with current username
     * @param newUserName new username
     * 
     * @return true if successful
     */
    public boolean renameUser(User user, String newUserName) {
        boolean success = false;
        
        if (userMap.containsKey(user)) {
            
            // Check if the username is in use
            if (!userNameInUse(newUserName)) {
                String oldUserName = user.getName();
                user.setName(newUserName);
                logger.log(Level.INFO, "{0} is now known as {1}", 
                        new Object[]{oldUserName, newUserName});
                success = true;
            }
        }
        
        return success;
    }
    
    /**
     * Removes a user from the database
     * 
     * @param user the user to be removed
     * 
     * @return true if successful
     */
    public boolean removeUser(User user) {
        
        boolean success = false;
        
        if (userMap.containsKey(user)) {
            Socket userSocket = getSocketForUser(user);
            userMap.remove(user);
            socketMap.remove(userSocket);
            socketEncryptionMap.remove(userSocket);
            logger.log(Level.INFO, "{0} has been removed", 
                    new Object[]{user});
            success = true;
        }
        
        return success;
    }
    
    /**
     * Used to see if a user name is in use
     * @param userName
     * @return true if the user name is in use
     */
    public boolean userNameInUse(String userName) {
        boolean inUse = false;
        
        for (User user : userMap.keySet()) {
            if (user.getName().equals(userName)) {
                inUse = true;
                break;
            }
        }
        
        return inUse;
    }
    
    /**
     * Returns the socket associated with the User
     * 
     * @param user the user
     * 
     * @return the user's socket
     */
    public Socket getSocketForUser(User user) {
        return userMap.get(user);
    }
    
    /**
     * Returns the user's socket
     * 
     * @param userName the user
     * 
     * @return the user's socket
     */
    public Socket getSocketForUser(String userName) {
        Socket socket = null;
        
        for (User user : userMap.keySet()) {
            if (user.getName().equals(userName)) {
                socket = userMap.get(user);
                break;
            }
        }
        return socket;
    }
    
    /**
     * Returns the user using the specified socket
     * 
     * @param socket the user's socket
     * 
     * @return the user
     */
    public User getUserOfSocket(Socket socket) {
        return socketMap.get(socket);
    }
    
    /**
     * Returns the address of the user
     * 
     * @param user
     * @return The address of the user
     */
    public String getAddressOfUserName(User user) {
        return userMap.get(user).getInetAddress().getHostName();
    }

    /**
     * Returns the whole userlist as User objects
     * 
     * @return Copy of the user list 
     */
    public List<User> getUserList() {
        return new ArrayList<>(userMap.keySet());
    }

    /**
     * Returns the whole socketlist
     * 
     * @return List of sockets
     */
    public List<Socket> getSocketList() {
        return new ArrayList<>(socketMap.keySet());
    }
    
    /**
     * Returns the socket list of the specified users
     * 
     * @param userList The user list
     * 
     * @return List of sockets corresponding to the lists of users
     */
    public List<Socket> getSocketListByUsers(List<User> userList) {
        List<Socket> socketList = new ArrayList<>();
        for (User user : userList) {
            socketList.add(UserDatabase.this.getSocketForUser(user));
        }
        return socketList;
    }

    public void clearHandlers(Socket socket) {
        User user = socketMap.get(socket);
        if (user != null) {
            user.clearHandlers();
        }
    }

    public void addHandler(Socket socket, BufferHandler handler) {
        User user = socketMap.get(socket);
        if (user != null) {
            user.addHandler(handler);
        }
    }

    public void setSocketIsEncrypted(Socket socket, boolean isEncrypted) {
        socketEncryptionMap.put(socket, isEncrypted);
    }
    
    public boolean isSocketEncrypted(Socket socket) {
        return socketEncryptionMap.get(socket);
    }
    
    /**
     * Get the secret key associated with the socket object
     * @param socket
     * @return Key of the socket/user or null if not found
     */
    public SecretKey getSecretKey(Socket socket) {
        SecretKey key = null;
        User user = getUserOfSocket(socket);
        if (user != null) {
            key = user.getSecretKey();
        }
        return key;
    }
}
