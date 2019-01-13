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
package com.lttldrgn.portochat.client;

import com.lttldrgn.portochat.common.User;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central place to manage data from the server.
 */
public class ServerDataStorage {
    private static ServerDataStorage instance;
    private final CopyOnWriteArrayList<UserEventListener> userEventListeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, User> userIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> nameToUser = new ConcurrentHashMap<>();

    /**
     * Get the singleton instance of the ServerDataStorage
     * @return ServerDataStorage singleton
     */
    public static synchronized ServerDataStorage getInstance() {
        if (instance == null) {
            instance = new ServerDataStorage();
        }
        return instance;
    }

    public void addUserEventListener(UserEventListener listener) {
        userEventListeners.addIfAbsent(listener);
    }

    public void removeUserEventListener(UserEventListener listener) {
        userEventListeners.remove(listener);
    }

    /**
     * Add user information to the storage and notify listeners
     * @param user User information
     */
    public void addUser(User user) {
        userIdMap.putIfAbsent(user.getId(), user);
        nameToUser.putIfAbsent(user.getName(), user);
        for (UserEventListener listener : userEventListeners) {
            listener.userAdded(user);
        }
    }

    /**
     * Add a list of users and notify listeners
     * @param users Users to add
     */
    public void addUsers(List<User> users) {
        for (User user : users) {
            addUser(user);
        }
    }

    /**
     * Remove user from storage and notify listeners
     * @param userId 
     */
    public void removeUser(String userId) {
        User user = userIdMap.remove(userId);
        if (user != null) {
            nameToUser.remove(user.getName());
            for (UserEventListener listener : userEventListeners) {
                listener.userRemoved(user);
            }
        }
    }

    /**
     * Get the user with the ID
     * @param userId ID of the user
     * @return User object or null if not found
     */
    public User getUserById(String userId) {
        return userIdMap.get(userId);
    }

    /**
     * Get a user by name
     * @param username User name
     * @return User object or null if not found
     */
    public User getUserByName(String username) {
        return nameToUser.get(username);
    }

    public void reset() {
        userIdMap.clear();
    }
}
