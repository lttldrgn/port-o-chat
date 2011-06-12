/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package portochat.common.socket.event;

import java.util.EventListener;

/**
 *
 * @author Mike
 */
public interface NetListener extends EventListener {

    public void incomingMessage(NetEvent event);

}
