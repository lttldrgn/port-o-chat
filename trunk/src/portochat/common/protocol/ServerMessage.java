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
    private String message = null;
    
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
            StringBuilder sb = new StringBuilder();         
            int messageLength = dis.readInt();
            for (int i = 0;i < messageLength;i++) {
                sb.append((char)dis.readUnsignedByte());
            }
            message = sb.toString();
            dis.readByte();
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
            dos.writeInt(message.length());
            for (int i = 0;i < message.length();i++) {
                dos.writeByte(message.charAt(i));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        
        return dos.size();
    }

    /**
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message
     * 
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Server Message: ");
        sb.append(message);
        
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
