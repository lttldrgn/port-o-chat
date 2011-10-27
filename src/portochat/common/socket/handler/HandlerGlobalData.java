/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.socket.handler;

import java.net.Socket;
import java.util.Map;

/**
 *
 * @author Mike
 */
public class HandlerGlobalData extends BufferHandler {

    @Override
    public boolean processIncoming(Socket socket, byte[] buffer, int length) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] processOutgoing(Socket socket, byte[] buffer, int length) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
