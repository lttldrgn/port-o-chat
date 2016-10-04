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
package com.lttldrgn.portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.lttldrgn.portochat.common.User;

/**
 * This class holds data for users connecting and disconnecting from the server.
 * 
 * @author Mike
 */
public class UserConnectionStatus extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserConnectionStatus.class.getName());
    private User user = null;
    private boolean connected = false;
    
    /*
     * Public constructor
     */
    public UserConnectionStatus() {
    }

    /**
     * Parses the data input stream
     * 
     * @param dis the data input stream
     */
    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);

        try {
            user = new User(dis);
            connected = dis.readBoolean();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    /*
     * Writes the data to the data output stream
     * 
     * @param dos The data output stream
     */
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            User.writeDos(user, dos);
            dos.writeBoolean(connected);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /*
     * Sets the user
     * 
     * @param user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /*
     * Sets if the user is connected
     * 
     * @param connected true if connected
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" User Connection: ");
        sb.append(user);
        sb.append((connected?" has connected!":" has disconnected!"));
        return sb.toString();
    }

    /*
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return "UserConnection";
    }
}
