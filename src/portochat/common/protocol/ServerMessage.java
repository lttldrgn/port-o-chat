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
public class ServerMessage extends DefaultData {

    private static final Logger logger = Logger.getLogger(ServerMessage.class.getName());
    private String message = null;
    
    public ServerMessage() {
    }
    
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Server Message: ");
        sb.append(message);
        
        return sb.toString();
    }

    @Override
    public String getObjectName() {
        return "ServerMessage";
    }
}
