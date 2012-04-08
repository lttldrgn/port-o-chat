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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds data for the initialization data.
 * @author Mike
 */
public class Initialization extends DefaultData {

    private static final Logger logger = Logger.getLogger(Initialization.class.getName());
    private String version = "1.0.0";
    private boolean server = false;
    private InitializationEnum initializationEnum = null;
    private byte[] encodedEncryptedSecretKey = null;
    private byte[] encodedPublicKey = null;
    
    /**
     * Public constructor
     */
    public Initialization() {
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
            version = dis.readUTF();
            server = dis.readBoolean();
            initializationEnum = InitializationEnum.get(dis.readInt());
            if (server) {
                // Server
                if (initializationEnum == InitializationEnum.ENCRYPTION_ON) {
                    int encodedLength = dis.readInt();
                    encodedEncryptedSecretKey = new byte[encodedLength];
                    dis.read(encodedEncryptedSecretKey);
                }
            } else {
                // Client
                if (initializationEnum == InitializationEnum.CLIENT_RSA_PRIVATE_KEY) {
                    int encodedLength = dis.readInt();
                    encodedPublicKey = new byte[encodedLength];
                    dis.read(encodedPublicKey);
                }
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
            dos.writeUTF(version);
            dos.writeBoolean(server);
            dos.writeInt(initializationEnum.getValue());
            if (server) {
                if (initializationEnum == InitializationEnum.ENCRYPTION_ON) {
                    dos.writeInt(encodedEncryptedSecretKey.length);
                    dos.write(encodedEncryptedSecretKey); 
                }
            } else {
                // Client
                if (initializationEnum == InitializationEnum.CLIENT_RSA_PRIVATE_KEY) {
                    dos.writeInt(encodedPublicKey.length);
                    dos.write(encodedPublicKey);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        
        return dos.size();
    }

    /**
     * @return The version as a string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version
     * 
     * @param version 
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return true if the node says they're a server
     */
    public boolean isServer() {
        return server;
    }

    /**
     * Used to differentiate clients from servers
     * 
     * @param server Set true if you are the server
     */
    public void setServer(boolean server) {
        this.server = server;
    }
    
    /**
     * @return The current InitializationEnum stage
     */
    public InitializationEnum getInitializationEnum() {
        return initializationEnum;
    }

    /**
     * Sets the InitializationEnum
     * 
     * @param initializationEnum 
     */
    public void setInitializationEnum(InitializationEnum initializationEnum) {
        this.initializationEnum = initializationEnum;
    }

    /**
     * @return Gets the encoded (and encrypted) secret key
     */
    public byte[] getEncodedEncryptedSecretKey() {
        return encodedEncryptedSecretKey;
    }

    /**
     * Sets the encoded (and encrypted) secret key
     * 
     * @param encodedEncryptedSecretKey
     */
    public void setEncodedEncryptedSecretKey(byte[] encodedEncryptedSecretKey) {
        this.encodedEncryptedSecretKey = encodedEncryptedSecretKey;
    }

    /**
     * @return Gets the encoded public key (used by servers)
     */
    public byte[] getEncodedPublicKey() {
        return encodedPublicKey;
    }

    /**
     * Sets the encoded public key (set by clients)
     * @param encodedPublicKey The encoded public key
     */
    public void setEncodedPublicKey(byte[] encodedPublicKey) {
        this.encodedPublicKey = encodedPublicKey;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(super.toString());
        sb.append("\n");
        sb.append("Initialization: ");
        sb.append(initializationEnum);
        if (initializationEnum == InitializationEnum.ENCRYPTION_ON) {
            sb.append("\n");
            sb.append("SecretKey: ");
            sb.append(encodedEncryptedSecretKey);
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * @return the object name
     */
    @Override
    public String getObjectName() {
        return "Initialization";
    }
}
