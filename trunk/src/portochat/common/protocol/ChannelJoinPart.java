/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class ChannelJoinPart extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelJoinPart.class.getName());
    private String user = null;
    private String channel = null;
    private boolean joined = false;
    
    public ChannelJoinPart() {
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);

        try {
            StringBuilder sb = new StringBuilder();
            int userLength = dis.readInt();
            for (int i = 0; i < userLength; i++) {
                sb.append((char) dis.readUnsignedByte());
            }
            user = sb.toString();
            sb = new StringBuilder();
            int channelLength = dis.readInt();
            for (int i = 0; i < channelLength; i++) {
                sb.append((char) dis.readUnsignedByte());
            }
            channel = sb.toString();
            joined = dis.readBoolean();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            if (user != null) {
                dos.writeInt(user.length());
                for (int i = 0; i < user.length(); i++) {
                    dos.writeByte(user.charAt(i));
                }
            } else {
                // Filled out by server.
                dos.writeInt(0);
            }
            dos.writeInt(channel.length());
            for (int i = 0; i < channel.length(); i++) {
                dos.writeByte(channel.charAt(i));
            }
            dos.writeBoolean(joined);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean hasJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" User: ");
        sb.append(user);
        sb.append((joined?" has joined ":" has parted "));
        sb.append(channel);
        return sb.toString();
    }

    @Override
    public String getObjectName() {
        return "ChannelJoinPart";
    }
}
