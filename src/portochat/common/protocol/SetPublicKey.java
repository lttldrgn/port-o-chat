/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message to set the client public key on the server
 * @author Brandon
 */
public class SetPublicKey extends DefaultData {
    private byte[] encodedPublicKey;
    
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeInt(encodedPublicKey.length);
            dos.write(encodedPublicKey);
        } catch (IOException ex) {
            Logger.getLogger(SetPublicKey.class.getName()).log(Level.SEVERE, "Error writing public key data", ex);
        }
        
        return dos.size();
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);
        try {
            int keyLength = dis.readInt();
            byte keyBytes[] = new byte[keyLength];
            dis.read(keyBytes);
            encodedPublicKey = keyBytes;
        } catch (IOException ex) {
            Logger.getLogger(SetPublicKey.class.getName()).log(Level.SEVERE, "Error parsing key", ex);
        }
    }

    public byte[] getEncodedPublicKey() {
        return encodedPublicKey;
    }

    public void setEncodedPublicKey(byte[] encodedPublicKey) {
        this.encodedPublicKey = encodedPublicKey;
    }
    
    
}
