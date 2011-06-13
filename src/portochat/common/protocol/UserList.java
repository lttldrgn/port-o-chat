/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author Mike
 */
public class UserList extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserList.class.getName());
    private List<String> userList = null;
    private String channel = null;

    public UserList() {
    }

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

    public List<String> getUserList() {
        return userList;
    }

    public void setUserList(List<String> userList) {
        this.userList = userList;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
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

    @Override
    public String getObjectName() {
        return "UserList";
    }
}
