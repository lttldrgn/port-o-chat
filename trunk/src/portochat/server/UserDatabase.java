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
package portochat.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a singleton class used to contain the user database.
 * 
 * @author Mike
 */
public class UserDatabase {

    private static final Logger logger = Logger.getLogger(UserDatabase.class.getName());
    private static UserDatabase instance = null;
    private Map<String, Socket> userMap = null;
    private Map<Socket, String> socketMap = null;
    
    /**
     * Private constructor.
     */
    private UserDatabase() {
        userMap = new ConcurrentHashMap<String, Socket>();
        socketMap = new ConcurrentHashMap<Socket, String>();
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
    
    /**
     * Adds a user to the database
     * 
     * @param user user to add
     * @param socket the user's socket. 
     * 
     * @return true if successful
     */
    public boolean addUser(String user, Socket socket) {
        boolean success = false;
        
        if (!userMap.containsKey(user)) {
            userMap.put(user, socket);
            socketMap.put(socket, user);
            logger.log(Level.INFO, "{0}@{1} has connected", 
                    new Object[]{user, socket.getInetAddress()});
            success = true;
        }
        
        return success;
    }
    
    /**
     * Renames a user in the database.
     * 
     * @param oldUser old username
     * @param newUser new username
     * @param socket the user's socket
     * 
     * @return true if successful
     */
    public boolean renameUser(String oldUser, String newUser, Socket socket) {
        boolean success = false;
        
        if (userMap.containsKey(oldUser)) {
            userMap.remove(oldUser);
            socketMap.remove(socket);
            userMap.put(newUser, socket);
            socketMap.put(socket, newUser);
            logger.log(Level.INFO, "{0}@{1} has renamed to {2}", 
                    new Object[]{oldUser, socket.getInetAddress(), newUser});
            success = true;
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
    public boolean removeUser(String user) {
        
        boolean success = false;
        
        if (userMap.containsKey(user)) {
            Socket userSocket = getUserOfSocket(user);
            userMap.remove(user);
            socketMap.remove(userSocket);
            success = true;
        }
        
        return success;
    }
    
    /**
     * Returns the user's socket
     * 
     * @param user the user
     * 
     * @return the user's socket
     */
    public Socket getUserOfSocket(String user) {
        return userMap.get(user);
    }
    
    /**
     * Returns the socket's user
     * 
     * @param socket the user's socket
     * 
     * @return the user
     */
    public String getSocketOfUser(Socket socket) {
        return socketMap.get(socket);
    }
    
    /**
     * Returns the whole userlist
     * 
     * @return List<String> of the userlist
     */
    public List<String> getUserList() {
        return new ArrayList<String>(userMap.keySet());
    }
    
    /**
     * Returns the whole socketlist
     * 
     * @return List<Socket> of the socketlist
     */
    public List<Socket> getSocketList() {
        return new ArrayList<Socket>(socketMap.keySet());
    }
    
    /**
     * Returns the socket list of the specified users
     * 
     * @param userList The userlist
     * 
     * @return List<Socket> of the userlist's sockets.
     */
    public List<Socket> getSocketList(List<String> userList) {
        List<Socket> socketList = new ArrayList<Socket>();
        for (String user : userList) {
            socketList.add(getUserOfSocket(user));
        }
        return socketList;
    }
    
}
