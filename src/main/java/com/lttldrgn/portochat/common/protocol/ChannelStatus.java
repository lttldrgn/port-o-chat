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

/**
 * This class holds data for channel creations and deletions.
 * 
 * @author Mike
 */
public class ChannelStatus extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelStatus.class.getName());
    private String channel = null;
    private boolean created = false;
    
    /**
     * Public constructor
     */
    public ChannelStatus() {
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
            channel = dis.readUTF();
            created = dis.readBoolean();
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
            dos.writeUTF(channel);
            dos.writeBoolean(created);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    /**
     * @return the channel
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
     * @return true if the channel was created, false if it was deleted
     */
    public boolean isCreated() {
        return created;
    }

    /**
     * Sets if the channel was created
     * 
     * @param created true if the channel was created, false if it was deleted
     */
    public void setCreated(boolean created) {
        this.created = created;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Channel Status: ");
        sb.append(channel);
        sb.append((created?" has been created.":" has been removed."));
        return sb.toString();
    }

    /**
     * the object name
     */
    @Override
    public String getObjectName() {
        return "ChannelStatus";
    }
}
