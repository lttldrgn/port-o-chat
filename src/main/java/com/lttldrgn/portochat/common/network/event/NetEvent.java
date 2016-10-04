/*
 *  This file is a part of port-o-chat.
 * 
 *  port-o-chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lttldrgn.portochat.common.network.event;

import java.util.EventObject;
import com.lttldrgn.portochat.common.protocol.DefaultData;

/**
 *
 * @author Mike
 */
public class NetEvent extends EventObject {

    private DefaultData defaultData = null;

    /*
     * Public constructor
     */
    public NetEvent (Object source, DefaultData defaultData) {
        super(source);
        this.defaultData = defaultData;
    }

    /***
     * @return The DefaultData object
     */
    public DefaultData getData() {
        return defaultData;
    }

}
