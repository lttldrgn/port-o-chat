/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lttldrgn.portochat.common.protocol.request;

import java.io.DataOutputStream;
import com.lttldrgn.portochat.common.protocol.DefaultData;

/**
 *
 * @author Brandon
 */
public class UserListRequest extends DefaultData {

    @Override
    public int writeBody(DataOutputStream dos) {
        return dos.size();
    }
    
}
