/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class UserList extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserList.class.getName());
    private List<String> userList = null;
    
    public UserList() {
    }
    
    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);

        try {
            userList = new ArrayList<String>();
            int numUsers = dis.readInt();
            for (int i = 0;i < numUsers;i++) {
                int userLength = dis.readInt();

                StringBuilder sb = new StringBuilder();
                for (int j = 0;j < userLength;j++) {
                    sb.append((char)dis.readUnsignedByte());
                }
                String user = sb.toString();
                userList.add(user);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }
    
    @Override
    public int writeBody(DataOutputStream dos) {
        
        try {
            // The server fills this out, from the client this will be null.
            if (userList == null) {
                dos.writeInt(0);
            } else {
                dos.writeInt(userList.size());
                for (String user : userList) {
                    dos.writeInt(user.length());
                    for (int i = 0;i < user.length();i++) {
                        dos.writeByte(user.charAt(i));
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        
        return dos.size();
    }

    public List<String> getUserList() {
        return userList;
    }

    public void setUserList(List<String> userList) {
        this.userList = userList;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" Num Users: ");
        if (userList != null) {
            sb.append(userList.size());
            for (String user : userList) {
                sb.append(" ");
                sb.append(user);
            }
        } else {
            sb.append("0");
        }
        
        return sb.toString();
    }

    @Override
    public String getName() {
        return "UserList";
    }
}
