/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.DataOutputStream;

/**
 * Indicates that the client has accepted the server secret key and can start
 * encrypting messages.
 * @author Brandon
 */
public class ServerKeyAccepted extends DefaultData {

    @Override
    public int writeBody(DataOutputStream dos) {
        return dos.size();
    }
    
}
