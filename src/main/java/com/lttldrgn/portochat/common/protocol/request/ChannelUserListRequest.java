/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lttldrgn.portochat.common.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.lttldrgn.portochat.common.protocol.DefaultData;

/**
 *
 * @author Brandon
 */
public class ChannelUserListRequest extends DefaultData {
    private String channelName;
    
    public ChannelUserListRequest() {
        // empty constructor needed for ProtocolHandler reflection
    }
    
    public ChannelUserListRequest(String channelName) {
        this.channelName = channelName;
    }
    
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeUTF(channelName);
        } catch (IOException ex) {
            Logger.getLogger(ChannelUserListRequest.class.getName()).log(Level.SEVERE, "Error writing channel name", ex);
        }
        return dos.size();
    }

    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);
        try {
            channelName = dis.readUTF();
        } catch (IOException ex) {
            Logger.getLogger(ChannelUserListRequest.class.getName()).log(Level.SEVERE, "Error reading channel name", ex);
        }
    }

    public String getChannelName() {
        return channelName;
    }
}
