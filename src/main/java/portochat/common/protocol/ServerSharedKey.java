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
 * Message used to transmit shared server-client encrypted key
 * @author Brandon
 */
public class ServerSharedKey extends DefaultData {
    byte encryptedSecretKey[];
    
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeInt(encryptedSecretKey.length);
            dos.write(encryptedSecretKey);
        } catch (IOException ex) {
            Logger.getLogger(ServerSharedKey.class.getName()).log(Level.SEVERE, "Error writing shared key", ex);
        }
        return dos.size();
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);
        try {
            int dataLength = dis.readInt();
            byte sharedKey[] = new byte[dataLength];
            dis.read(sharedKey);
            encryptedSecretKey = sharedKey;
        } catch (IOException ex) {
            Logger.getLogger(ServerSharedKey.class.getName()).log(Level.SEVERE, "Error reading shared key", ex);
        }
    }

    public byte[] getEncryptedSecretKey() {
        return encryptedSecretKey;
    }

    public void setEncryptedSecretKey(byte[] encryptedSecretKey) {
        this.encryptedSecretKey = encryptedSecretKey;
    }
}
