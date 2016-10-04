/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lttldrgn.portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message sent by the server if the client tries to send a message to a recipient
 * that is not connected to the server.
 * @author Brandon
 */
public class UserDoesNotExist extends DefaultData {
    private String user;
    
    public UserDoesNotExist() {
        
    }
    
    public UserDoesNotExist(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);
        try {
            user = dis.readUTF();
        } catch (IOException ex) {
            Logger.getLogger(UserDoesNotExist.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            dos.writeUTF(user);
        } catch (IOException ex) {
            Logger.getLogger(UserDoesNotExist.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dos.size();
    }

    @Override
    public String toString() {
        return "UserDoesNotExist{" + "user=" + user + '}';
    }
    
    
}
