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
package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds data for the server's channel list.
 * 
 * @author Mike
 */
public class ChannelList extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelList.class.getName());
    private List<String> channelList = null;

    /**
     * Public constructor
     */
    public ChannelList() {
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
            int numChannels = dis.readInt();
            channelList = new ArrayList<String>();
            for (int i = 0; i < numChannels; i++) {
                
                int channelLength = dis.readInt();

                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < channelLength; j++) {
                    sb.append((char) dis.readUnsignedByte());
                }
                String channel = sb.toString();
                channelList.add(channel);
            }
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
            // The server fills this out, from the client this will be null.
            if (channelList == null || channelList.size() == 0) {
                dos.writeInt(0);
            } else {
                dos.writeInt(channelList.size());
                for (String channel : channelList) {
                    dos.writeInt(channel.length());
                    for (int i = 0; i < channel.length(); i++) {
                        dos.writeByte(channel.charAt(i));
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    /**
     * @return a List<String> containing all the channels in the list
     */
    public List<String> getChannelList() {
        return channelList;
    }

    /**
     * Sets the channel list
     * 
     * @param channelList
     */
    public void setChannelList(List<String> channelList) {
        this.channelList = channelList;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Num Channels: ");
        if (channelList != null) {
            sb.append(channelList.size());
            for (String channel : channelList) {
                sb.append(" ");
                sb.append(channel);
            }
        } else {
            sb.append("0");
        }

        return sb.toString();
    }

    /**
     * the object name
     */
    @Override
    public String getObjectName() {
        return "ChannelList";
    }
}
