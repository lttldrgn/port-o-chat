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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds data for a server message.
 * 
 * @author Mike
 */
public class ServerMessage extends DefaultData {

    private static final Logger logger = Logger.getLogger(ServerMessage.class.getName());
    private String additionalMessage = null;
    private ServerMessageEnum messageEnum;
    
    /**
     * The public constructor
     */
    public ServerMessage() {
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
            messageEnum = ServerMessageEnum.values()[dis.readInt()];
            boolean hasAdditionalMessage = dis.readBoolean();
            if (hasAdditionalMessage) {
                additionalMessage = dis.readUTF();
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
            dos.writeInt(messageEnum.getValue());
            dos.writeBoolean(additionalMessage != null);
            if (additionalMessage != null) {
                dos.writeUTF(additionalMessage);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        
        return dos.size();
    }

    /**
     * @see ServerMessage#setAdditionalMessage(java.lang.String) 
     * @return The message
     */
    public String getAdditionalMessage() {
        return additionalMessage;
    }

    /**
     * Sets the message
     * 
     * @param message the message
     * @see ServerMessage#getAdditionalMessage()
     */
    public void setAdditionalMessage(String message) {
        this.additionalMessage = message;
    }

    public ServerMessageEnum getMessageEnum() {
        return messageEnum;
    }

    public void setMessageEnum(ServerMessageEnum messageEnum) {
        this.messageEnum = messageEnum;
    }
    
    /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Server Message: ");
        sb.append(messageEnum);
        sb.append("\n");
        sb.append(additionalMessage);
        
        return sb.toString();
    }

    /**
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return "ServerMessage";
    }
}
