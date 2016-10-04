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
package portochat.common.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.protocol.DefaultData;

/**
 * This class holds data for requesting a username.
 * 
 * @author Mike
 */
public class SetUsernameRequest extends DefaultData {

    private static final Logger logger = Logger.getLogger(SetUsernameRequest.class.getName());
    private String name = null;

    /**
     * Public constructor
     */
    public SetUsernameRequest() {
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
            name = dis.readUTF();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeUTF(name);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }

        return dos.size();
    }

    /**
     * @return the user
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user
     * 
     * @param user
     */
    public void setUser(String user) {
        this.name = user;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("Name: ");
        sb.append(name);

        return sb.toString();
    }

    /**
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return "UserData";
    }
}
