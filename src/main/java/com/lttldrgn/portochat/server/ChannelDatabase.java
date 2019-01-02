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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.lttldrgn.portochat.common.User;

/**
 * This class handles the channel database.
 * 
 * @author Mike
 */
public class ChannelDatabase {

    private static final Logger logger = Logger.getLogger(ChannelDatabase.class.getName());
    private static ChannelDatabase instance = null;
    /** Map of Channel names to list of users */
    private Map<String, ArrayList<User>> channelMap = null;
    /** Map of User to Channels they are in */
    private Map<User, ArrayList<String>> userChannelMap = null;   
    private UserDatabase userDatabase = null;

    /**
     * Private constructor
     */
    private ChannelDatabase() {
        channelMap = new ConcurrentHashMap<>();
        userChannelMap = new ConcurrentHashMap<>();
        userDatabase = UserDatabase.getInstance();
    }

    /**
     * Method to get the instance of this singleton.
     * 
     * @return ProtocolHandler
     */
    public static synchronized ChannelDatabase getInstance() {
        if (instance == null) {
            instance = new ChannelDatabase();
        }
        return instance;
    }

    /**
     * Adds a user to a channel
     * 
     * @param channel
     * @param user
     */
    public void addUserToChannel(String channel, User user) {
        ArrayList<User> userList = channelMap.computeIfAbsent(channel, k -> {
            return new ArrayList<>();
        });
        userList.add(user);
        
        ArrayList<String> userChannelList = userChannelMap.computeIfAbsent(user, k -> {
            return new ArrayList<>();
        });
        userChannelList.add(channel);
    }

    /**
     * Removes a user from a channel
     * 
     * @param channel
     * @param user
     */
    public void removeUserFromChannel(String channel, User user) {
        ArrayList<User> userList = channelMap.get(channel);
        if (userList != null) {
            userList.remove(user);

            if (userList.isEmpty()) {
                // Remove from map
                channelMap.remove(channel);
            }
        } else {
            // Shouldn't happen
            logger.log(Level.SEVERE, "Unable to remove {0} from {1}'s user list",
                    new Object[]{user, channel});
        }

        // clean up user-channel associations
        ArrayList<String> userChannelList = userChannelMap.get(user);
        if (userChannelList != null) {
            userChannelList.remove(channel);
            
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

    /**
     * Rename a user in all his channels
     * 
     * @param oldUserName
     * @param newUserName 
     */
    public void renameUser(String oldUserName, String newUserName) {
        for (User user : userChannelMap.keySet()) {
            if (user.getName().equals(oldUserName)) {
                // Get list of channels
                ArrayList<String> channelList = userChannelMap.get(user);
                
                for (String channel : channelList) {
                    ArrayList<User> userList = channelMap.get(channel);
                    if (userList != null) {
                        Iterator iter = userList.iterator();
                        while (iter.hasNext()) {
                            if (((User) iter.next()).equals(user)) {
                                user.setName(newUserName);
                                break;
                            }
                        }
                    }
                }
                
                // Set new user name
                user.setName(newUserName);
                break;
            }
        }        
    }
    
    /**
     * Returns true if the user is in a channel
     * 
     * @param channel
     * @param user
     * 
     * @return true if the user is in the channel
     */
    public boolean isUserInChannel(String channel, User user) {
        boolean exists = false;
        
        ArrayList<User> userList = channelMap.get(channel);
        if (userList != null) {
            Iterator iter = userList.iterator();
            while (iter.hasNext()) {
                if (((User) iter.next()).equals(user)) {
                    exists = true;
                    break;
                }
            }
        }
        
        return exists;
    }
    
    /**
     * Returns all the channels a user is in.
     * 
     * @param user
     * 
     * @return a List containing the user's channels
     */
    public List<String> getUserChannels(User user) {
        List<String> channelList = userChannelMap.get(user);
        List<String> returnList = new ArrayList<>();
        if (channelList != null) {
            returnList.addAll(channelList);
        }
        return returnList;
    }
    
    /**
     * Removes the user from all channels they're in
     * 
     * @param user
     */
    public void removeUserFromAllChannels(User user) {
        for (String channel : channelMap.keySet()) {
            if (isUserInChannel(channel, user)) {
                removeUserFromChannel(channel, user);
            }
        }
    }
    
    /**
     * Returns true if the channel exists
     * 
     * @param channel
     * 
     * @return true if the channel exists
     */
    public boolean channelExists(String channel) {
        return channelMap.containsKey(channel);
    }
    
    /**
     * Returns the users in a channel
     * 
     * @param channel
     * 
     * @return a List containing the users in the channel
     */
    public List<User> getUsersInChannel(String channel) {
        return channelMap.get(channel);
    }
    
    /**
     * @return a List containing all the channels
     */
    public List<String> getListOfChannels() {
        return new ArrayList<>(channelMap.keySet());
    }

    /**
     * Gets the sockets of the users in a channel
     * 
     * @param channel
     * @param filterUser Used to filter out a user you don't want in the list
     *        (e.g. the client)
     * 
     * @return a List containing all the user's sockets
     */
    public List<Socket> getSocketsOfUsersInChannel(String channel, User filterUser) {
        ArrayList<Socket> userSocketList = null;
        ArrayList<User> userList = channelMap.get(channel);
        if (userList != null) {
            ArrayList<User> copyOfUserList = new ArrayList<>(userList);

            if (filterUser != null) {
                // Remove the sender's name
                Iterator iter = copyOfUserList.iterator();
                while (iter.hasNext()) {
                    if (((User) iter.next()).equals(filterUser)) {
                        iter.remove();
                        break;
                    }
                }
            }

            // List of sockets for each user in the channel
            userSocketList =
                    (ArrayList<Socket>) userDatabase.getSocketListByUsers(copyOfUserList);
        } else {
            // Shouldn't happen
            logger.log(Level.SEVERE,
                    "User '{0}' was unable to send message to channel named {1} beacuse it doesn't exist!",
                    new Object[]{filterUser, channel});
        }
        return userSocketList;
    }
}
