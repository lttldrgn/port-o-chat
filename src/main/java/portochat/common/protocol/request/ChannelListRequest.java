/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol.request;

import java.io.DataOutputStream;
import portochat.common.protocol.DefaultData;

/**
 * Requests the channel list from the server
 * @author Brandon
 */
public class ChannelListRequest extends DefaultData {

    @Override
    public int writeBody(DataOutputStream dos) {
        return dos.size();
    }
    
}
