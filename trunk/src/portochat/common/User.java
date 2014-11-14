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
package portochat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.crypto.SecretKey;
import portochat.common.protocol.InitializationEnum;
import portochat.common.socket.handler.BufferHandler;

/**
 *
 * @author Mike
 */
public class User {

    private String name = null;
    private String host = null;
    private long lastSeen;
    private SecretKey secretKey = null;
    private PublicKey clientPublicKey = null;
    private InitializationEnum initEnum = null;
    private List<BufferHandler> handlers = null;
    
    /**
     * User constructor
     */
    public User() {
        handlers = new CopyOnWriteArrayList<BufferHandler>();
    }
    
    /**
     * Constructor using a DataInputStream to parse in user data
     * 
     * @param dis
     * @throws IOException 
     */
    public User(DataInputStream dis) throws IOException {
        boolean okToRead = dis.readBoolean();
        if (okToRead) {
            name = dis.readUTF();
            host = dis.readUTF();
        }
    }

    /**
     * @return Returns the user's host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the user's host
     * 
     * @param host 
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return Returns the user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name
     * 
     * @param name 
     */
    public void setName(String name) {
        this.name = name;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public PublicKey getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(PublicKey clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public InitializationEnum getInitEnum() {
        return initEnum;
    }

    public void setInitEnum(InitializationEnum initEnum) {
        this.initEnum = initEnum;
    }

    public void clearHandlers() {
        handlers.clear();
    }

    public List<BufferHandler> getHandlers() {
        return handlers;
    }
      
    public void addHandler(BufferHandler handler) {
        handlers.add(handler);
    }
    
    public void removeHandler(BufferHandler handler) {
        handlers.remove(handler);
    }
    
    /**
     * Static method used to write out user data to the DataOutputStream
     * 
     * @param user
     * @param dos 
     */
    public static void writeDos(User user, DataOutputStream dos) throws IOException {
        if (user != null) {
            dos.writeBoolean(true); //bit to see if we should read user information
            dos.writeUTF(user.getName());
            dos.writeUTF(user.getHost());
        } else {
            dos.writeBoolean(false);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 83 * hash + (this.host != null ? this.host.hashCode() : 0);
        return hash;
    }
    
    @Override
    public String toString() {
        return name + " (" + host + ")";
    }

}
