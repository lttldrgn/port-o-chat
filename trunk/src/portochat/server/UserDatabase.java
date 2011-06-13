/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class UserDatabase {

    private static final Logger logger = Logger.getLogger(UserDatabase.class.getName());
    private static UserDatabase instance = null;
    private Map<String, Socket> userMap = null;
    private Map<Socket, String> socketMap = null;
    
    private UserDatabase() {
        userMap = new ConcurrentHashMap<String, Socket>();
        socketMap = new ConcurrentHashMap<Socket, String>();
    }
    
    public synchronized static UserDatabase getInstance() {
        if (instance == null) {
            instance = new UserDatabase();
        }
        return instance;
    }
    
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
    
    public Socket getUserOfSocket(String user) {
        return userMap.get(user);
    }
    
    public String getSocketOfUser(Socket socket) {
        return socketMap.get(socket);
    }
    
    public List<String> getUserList() {
        return new ArrayList<String>(userMap.keySet());
    }
    
    public List<Socket> getSocketList() {
        return new ArrayList<Socket>(socketMap.keySet());
    }
    
    public List<Socket> getSocketList(List<String> userList) {
        List<Socket> socketList = new ArrayList<Socket>();
        for (String user : userList) {
            socketList.add(getUserOfSocket(user));
        }
        return socketList;
    }
    
}
