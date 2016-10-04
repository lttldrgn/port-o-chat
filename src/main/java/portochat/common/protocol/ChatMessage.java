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
import portochat.common.User;

/**
 * This class holds client messages to a user or channel.
 * 
 * @author Mike
 */
public class ChatMessage extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChatMessage.class.getName());
    private User fromUser = null;
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
            fromUser = new User(dis);
            to = dis.readUTF();
            
            action = dis.readBoolean();
            message = dis.readUTF();
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
            User.writeDos(fromUser, dos);
            dos.writeUTF(to);
            dos.writeBoolean(action);
            dos.writeUTF(message);
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
    public User getFromUser() {
        return fromUser;
    }

    /**
     * Sets the from user
     * 
     * @param fromUser
     */
    public void setFromUser(User fromUser) {
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
        sb.append("ChatMessage - ");
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
