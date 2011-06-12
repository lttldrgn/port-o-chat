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
public class ChatMessage extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChatMessage.class.getName());
    private String fromUser = null;
    private String toUser = null;
    private String message = null;
    
    public ChatMessage() {
    }
    
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
            toUser = sb.toString();
            
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
            
            dos.writeInt(toUser.length());
            for (int i = 0;i < toUser.length();i++) {
                dos.writeByte(toUser.charAt(i));
            }
            
            dos.writeInt(message.length());
            for (int i = 0;i < message.length();i++) {
                dos.writeByte(message.charAt(i));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        
        return dos.size();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        length = message.length();
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }
    
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" ");
        sb.append(fromUser);
        sb.append(": ");
        sb.append(message);
        
        return sb.toString();
    }

    @Override
    public String getName() {
        return "ChatMessage";
    }
}
