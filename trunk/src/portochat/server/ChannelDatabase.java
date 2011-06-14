/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class ChannelDatabase {

    private static final Logger logger = Logger.getLogger(ChannelDatabase.class.getName());
    private static ChannelDatabase instance = null;
    private Map<String, ArrayList<String>> channelMap = null;
    private Map<String, ArrayList<String>> userChannelMap = null;
    
    private UserDatabase userDatabase = null;

    private ChannelDatabase() {
        channelMap = new ConcurrentHashMap<String, ArrayList<String>>();
        userChannelMap = new ConcurrentHashMap<String, ArrayList<String>>();
        userDatabase = UserDatabase.getInstance();
    }

    public static synchronized ChannelDatabase getInstance() {
        if (instance == null) {
            instance = new ChannelDatabase();
        }
        return instance;
    }

    public void addUserToChannel(String channel, String user) {
        ArrayList<String> userList = channelMap.get(channel);
        if (userList == null) {
            // New channel
            userList = new ArrayList<String>();
            channelMap.put(channel, userList);
        }
        userList.add(user);
        
        ArrayList<String> userChannelList = userChannelMap.get(user);
        if (userChannelList == null) {
            // First joined channel
            userChannelList = new ArrayList<String>();
            userChannelMap.put(user, userChannelList);
        }
        userChannelList.add(channel);
    }

    public void removeUserFromChannel(String channel, String user) {
        ArrayList<String> userList = channelMap.get(channel);
        if (userList != null) {
            Iterator iter = userList.iterator();
            while (iter.hasNext()) {
                if (((String) iter.next()).equals(user)) {
                    iter.remove();
                    break;
                }
            }

            if (userList.isEmpty()) {
                // Remove from map
                channelMap.remove(channel);
            }
        } else {
            // Shouldn't happen
            logger.log(Level.SEVERE, "Unable to remove {0} from {1}'s user list",
                    new Object[]{user, channel});
        }
        
        ArrayList<String> userChannelList = userChannelMap.get(user);
        if (userChannelList != null) {
            Iterator iter = userChannelList.iterator();
            while (iter.hasNext()) {
                if (((String) iter.next()).equals(channel)) {
                    iter.remove();
                    break;
                }
            }
            
            if (userChannelList.isEmpty()) {
                // Remove from map
                userChannelMap.remove(user);
            }
        } else {
            // Shouldn't happen
            logger.log(Level.SEVERE, "Unable to remove {0} from {1}'s channel list",
                    new Object[]{channel, user});
        }
        
    }

    public boolean isUserInChannel(String channel, String user) {
        boolean exists = false;
        
        ArrayList<String> userList = channelMap.get(channel);
        if (userList != null) {
            Iterator iter = userList.iterator();
            while (iter.hasNext()) {
                if (((String) iter.next()).equals(user)) {
                    exists = true;
                    break;
                }
            }
        }
        
        return exists;
    }
    
    public void removeUserFromAllChannels(String user) {
        for (String channel : channelMap.keySet()) {
            if (isUserInChannel(channel, user)) {
                removeUserFromChannel(channel, user);
            }
        }
    }
    
    public boolean channelExists(String channel) {
        return (channelMap.get(channel) != null);
    }
    
    public List<String> getUsersInChannel(String channel) {
        return channelMap.get(channel);
    }
    
    public List<String> getListOfChannels() {
        return new ArrayList<String>(channelMap.keySet());
    }

    public List<Socket> getSocketsOfUsersInChannel(String channel, String filterUser) {
        ArrayList<Socket> userSocketList = null;
        ArrayList<String> userList = channelMap.get(channel);
        if (userList != null) {
            ArrayList<String> copyOfUserList = new ArrayList<String>(channelMap.get(channel));
            if (copyOfUserList != null) {
                if (filterUser != null) {
                    // Remove the sender's name
                    Iterator iter = copyOfUserList.iterator();
                    while (iter.hasNext()) {
                        if (((String) iter.next()).equals(filterUser)) {
                            iter.remove();
                            break;
                        }
                    }
                }

                // List of sockets for each user in the channel
                userSocketList =
                        (ArrayList<Socket>) userDatabase.getSocketList(copyOfUserList);

            } else {
                // Shouldn't happen
                logger.log(Level.SEVERE,
                        "{0} was unable to send message to {1} beacuse it doesn't exist!",
                        new Object[]{filterUser, channel});
            }
        }
        return userSocketList;
    }
}
