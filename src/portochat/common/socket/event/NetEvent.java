/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package portochat.common.socket.event;

import java.util.EventObject;
import portochat.common.protocol.DefaultData;

/**
 *
 * @author Mike
 */
public class NetEvent extends EventObject {

    private DefaultData defaultData = null;

    public NetEvent (Object source, DefaultData defaultData) {
        super(source);
        this.defaultData = defaultData;
    }

    public DefaultData getData() {
        return defaultData;
    }

}
