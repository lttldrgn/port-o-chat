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
import portochat.common.User;

/**
 * This class holds data for user lists in a channel, or server wide.
 * @author Mike
 */
public class UserList extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserList.class.getName());
    private List<User> userList = null;
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
                channel = dis.readUTF();
            }
            int numUsers = dis.readInt();
            userList = new ArrayList<>(numUsers);
            
            for (int i = 0; i < numUsers; i++) {
                userList.add(new User(dis));
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
            if (channel != null) {
                dos.writeBoolean(true);
                dos.writeUTF(channel);
            } else {
                dos.writeBoolean(false);
            }

            if (userList != null) {
                dos.writeInt(userList.size());
                for (User user : userList) {
                    User.writeDos(user, dos);
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
     * @return A List&lt;User&gt; of the users
     */
    public List<User> getUserList() {
        return userList;
    }

    /*
     * Sets the user list
     * 
     * @param userList
     */
    public void setUserList(List<User> userList) {
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
            for (User user : userList) {
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
