/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class UserData extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserData.class.getName());
    private String user = null;

    public UserData() {
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
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeInt(user.length());
            for (int i = 0; i < user.length(); i++) {
                dos.writeByte(user.charAt(i));
            }
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

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("User: ");
        sb.append(user);

        return sb.toString();
    }

    @Override
    public String getObjectName() {
        return "UserData";
    }
}
