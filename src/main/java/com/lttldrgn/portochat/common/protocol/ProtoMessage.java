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

import com.lttldrgn.portochat.proto.Portochat;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a protobuf message
 * @author Brandon
 */
public class ProtoMessage extends DefaultData {
    private static Logger logger = Logger.getLogger(ProtoMessage.class.getName());

    private Portochat.PortoChatMessage message;

    public ProtoMessage() {
        // default constructor for reflective creation
    }

    public ProtoMessage(Portochat.PortoChatMessage message) {
        this.message = message;
    }

    /**
     * Get the protobuf message
     * @return Protobuf message object
     */
    public Portochat.PortoChatMessage getMessage() {
        return message;
    }

    @Override
    public int writeBody(DataOutputStream dos) {
       
        try {
            byte[] data = message.toByteArray();
            dos.writeInt(data.length);
            dos.write(data);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error encoding message", ex);
        }
        return dos.size();
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);
        try {
            int dataLength = dis.readInt();
            byte data[] = new byte[dataLength];
            int bytesRead = dis.read(data, 0, dataLength);
            if (bytesRead != dataLength) {
                logger.log(Level.SEVERE, "Bytes read doesn''t match length. Actual: {0} Expected: {1}", new Object[]{bytesRead, dataLength});
            }
            message = Portochat.PortoChatMessage.parseFrom(data);
        } catch (IOException ex) {
            Logger.getLogger(ProtoMessage.class.getName()).log(Level.SEVERE, "Error decoding message", ex);
        }
    }

    public void setCanBeEncrypted(boolean canBeEncrypted) {
        this.canBeEncrypted = canBeEncrypted;
    }
}
