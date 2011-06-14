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
public class ChannelStatus extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelStatus.class.getName());
    private String channel = null;
    private boolean created = false;
    
    public ChannelStatus() {
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);

        try {
            StringBuilder sb = new StringBuilder();
            int channelLength = dis.readInt();
            for (int i = 0; i < channelLength; i++) {
                sb.append((char) dis.readUnsignedByte());
            }
            channel = sb.toString();
            created = dis.readBoolean();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeInt(channel.length());
            for (int i = 0; i < channel.length(); i++) {
                dos.writeByte(channel.charAt(i));
            }
            dos.writeBoolean(created);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Channel Status: ");
        sb.append(channel);
        sb.append((created?" has been created.":" has been removed."));
        return sb.toString();
    }

    @Override
    public String getObjectName() {
        return "ChannelStatus";
    }
}
