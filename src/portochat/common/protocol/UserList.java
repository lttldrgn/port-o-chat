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
 * This class holds data for user lists in a channel, or server wide.
 * @author Mike
 */
public class UserList extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserList.class.getName());
    private List<String> userList = null;
    private String channel = null;

    /*
     * Public constructor
     */
    public UserList() {
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
            boolean isChannel = dis.readBoolean();
            if (isChannel) {
                int channelLength = dis.readInt();
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < channelLength; j++) {
                    sb.append((char) dis.readUnsignedByte());
                }
                channel = sb.toString();
            }
            int numUsers = dis.readInt();
            if (numUsers > 0) {
                userList = new ArrayList<String>();
            }
            for (int i = 0; i < numUsers; i++) {
                int userLength = dis.readInt();

                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < userLength; j++) {
                    sb.append((char) dis.readUnsignedByte());
                }
                String user = sb.toString();
                userList.add(user);
            }
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
            // The server fills this out, from the client this will be null.
            if (userList == null) {
                if (channel != null) {
                    // Requesting users from channel
                    dos.writeBoolean(true);
                    dos.writeInt(channel.length());
                    for (int i = 0; i < channel.length(); i++) {
                        dos.writeByte(channel.charAt(i));
                    }
                } else {
                    // Requseting users from server
                    dos.writeBoolean(false);
                    dos.writeInt(0);
                }
            } else if (channel != null) {
                dos.writeBoolean(true);
                dos.writeInt(channel.length());
                for (int i = 0; i < channel.length(); i++) {
                    dos.writeByte(channel.charAt(i));
                }
            } else {
                dos.writeBoolean(false);
            }

            if (userList != null) {
                dos.writeInt(userList.size());
                for (String user : userList) {
                    dos.writeInt(user.length());
                    for (int i = 0; i < user.length(); i++) {
                        dos.writeByte(user.charAt(i));
                    }
                }
            } else {
                // Filled out by server
                dos.writeInt(0);
            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    /**
     * @return A List<String> of the users
     */
    public List<String> getUserList() {
        return userList;
    }

    /*
     * Sets the user list
     * 
     * @param userList
     */
    public void setUserList(List<String> userList) {
        this.userList = userList;
    }

    /**
     * @return Returns the channel
     */
    public String getChannel() {
        return channel;
    }

    /*
     * Sets the channel
     * 
     * @param channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" UserList -");
        if (channel != null) {
            sb.append(" ");
            sb.append(channel);
        }
        sb.append(" Num Users: ");
        if (userList != null) {
            sb.append(userList.size());
            for (String user : userList) {
                sb.append(" ");
                sb.append(user);
            }
        } else {
            sb.append("0");
        }

        return sb.toString();
    }

    /*
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return "UserList";
    }
}
