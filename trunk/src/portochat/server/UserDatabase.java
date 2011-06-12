/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.net.Socket;
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
    
    public Socket getUserSocket(String user) {
        return userMap.get(user);
    }
    
    public String getSocketUser(Socket socket) {
        return socketMap.get(socket);
    }
}
