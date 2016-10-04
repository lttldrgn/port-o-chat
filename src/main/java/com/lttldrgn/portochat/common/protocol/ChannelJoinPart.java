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
 * This class contains information about a channel join/part event.  It includes
 * information about the channel, user and whether it is a join.
 * @author Mike
 */
public class ChannelJoinPart extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelJoinPart.class.getName());
    private User user = null;
    private String channel = null;
    private boolean joined = false;
    
    /**
     * Public constructor
     */
    public ChannelJoinPart() {
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
            channel = dis.readUTF();
            joined = dis.readBoolean();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    /**
     * Writes the data to the data output stream
     * 
     * @param dos The data output stream
     */
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            
            // The user data is filled out by the server.
            User.writeDos(user, dos);
            dos.writeUTF(channel);
            dos.writeBoolean(joined);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    /**
     * @return returns the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user
     * @param user 
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return The channel that the user is joining/parting
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the channel
     * 
     * @param channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return true if user is joining channel, false if leaving
     */
    public boolean hasJoined() {
        return joined;
    }

    /**
     * Sets if the user is joining the channel
     * 
     * @param joined true if the user is joining, false if he is parting
     */
    public void setJoined(boolean joined) {
        this.joined = joined;
    }

     /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" ChannelJoinPart - User: ");
        sb.append(user);
        sb.append((joined?" has joined ":" has parted "));
        sb.append(channel);
        return sb.toString();
    }

    /**
     * the object name
     */
    @Override
    public String getObjectName() {
        return "ChannelJoinPart";
    }
}
