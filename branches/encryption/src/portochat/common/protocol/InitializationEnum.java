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
package portochat.common.protocol;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Mike
 */
public enum InitializationEnum {
    WAITING_FOR_CLIENT(0, "Server is waiting for the client"),
    CLIENT_RSA_PRIVATE_KEY(1, "Client has sent their RSA public key."),
    ENCRYPTION_ON(2, "Server has encryption on."),
    ENCRYPTION_OFF(3, "Server has encryption off."),
    READY(4, "Ready.");

    private int value;
    private String message;
    private static final Map<Integer,InitializationEnum> lookup 
          = new HashMap<Integer,InitializationEnum>();
    
    static {
        for (InitializationEnum e : EnumSet.allOf(InitializationEnum.class)) {
            lookup.put(e.getValue(), e);
        }
    }

    InitializationEnum(int value, String message) {
        this.value = value;
        this.message = message;
    }

    /**
     * Returns the full string message of this enumeration
     * @return Full string message of the enumeration
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the assigned integer value of this enumeration, not to be 
     * confused with the natural ordinal value that Java assigns
     * @return integer value of this enumeration
     */
    public int getValue() {
        return value;
    }

    public static InitializationEnum get(int value) {
        return lookup.get(value);
    }

}