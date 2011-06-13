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
public class ChannelList extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelList.class.getName());
    private List<String> channelList = null;

    public ChannelList() {
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);

        try {
            int numChannels = dis.readInt();
            if (numChannels > 0) {
                channelList = new ArrayList<String>();
            }
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

    @Override
    public int writeBody(DataOutputStream dos) {

        try {
            // The server fills this out, from the client this will be null.
            if (channelList == null) {
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

    public List<String> getChannelList() {
        return channelList;
    }

    public void setChannelList(List<String> channelList) {
        this.channelList = channelList;
    }

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

    @Override
    public String getObjectName() {
        return "ChannelList";
    }
}
