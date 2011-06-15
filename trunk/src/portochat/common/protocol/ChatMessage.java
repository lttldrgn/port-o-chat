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
 * This class holds client messages to a user or channel.
 * 
 * @author Mike
 */
public class ChatMessage extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChatMessage.class.getName());
    private String fromUser = null;
    private String to = null;
    private boolean action = false;
    private String message = null;
    
    /**
     * Public constructor
     */
    public ChatMessage() {
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
            
            int userLength = dis.readInt();
            for (int i = 0;i < userLength;i++) {
                sb.append((char)dis.readUnsignedByte());
            }
            fromUser = sb.toString();
                        
            sb = new StringBuilder();
            userLength = dis.readInt();
            for (int i = 0;i < userLength;i++) {
                sb.append((char)dis.readUnsignedByte());
            }
            to = sb.toString();
            
            action = dis.readBoolean();
            int messageLength = dis.readInt();
            sb = new StringBuilder();
            for (int i = 0;i < messageLength;i++) {
                sb.append((char)dis.readUnsignedByte());
            }
            message = sb.toString();
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
            if (fromUser == null) {
                dos.writeInt(0);
            } else { 
                dos.writeInt(fromUser.length());
                for (int i = 0;i < fromUser.length();i++) {
                    dos.writeByte(fromUser.charAt(i));
                }
            }
            
            dos.writeInt(to.length());
            for (int i = 0;i < to.length();i++) {
                dos.writeByte(to.charAt(i));
            }
            
            dos.writeBoolean(action);
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
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message
     * 
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
        length = message.length();
    }

    /**
     * @return who this message is to (user or #channel).
     * Note: channels are prefixed with #
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets who this message is to (user or #channel).
     * Note: channels are prefixed with #
     * 
     * @param to
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * @return true if this message is going to be sent to a channel
     */
    public boolean isChannel() {
        if (to != null && to.startsWith("#")) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * @return the from user
     */
    public String getFromUser() {
        return fromUser;
    }

    /**
     * Sets the from user
     * 
     * @param fromUser
     */
    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    /**
     * @return true if this message is an action
     */
    public boolean isAction() {
        return action;
    }

    /**
     * Sets if this message is an action.
     */
    public void setAction(boolean action) {
        this.action = action;
    }
    
    /**
     * Overridden toString method
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" ");
        if (isChannel()) {
            sb.append(to);
            sb.append(" ");
        }
        if (action) {
            sb.append("* ");
        } else {
            sb.append("<");
        }
        sb.append(fromUser);
        if (action) {
            sb.append(" ");
        } else {
            sb.append("> ");
        }
        sb.append(message);
        
        return sb.toString();
    }

    /**
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return "ChatMessage";
    }
}
