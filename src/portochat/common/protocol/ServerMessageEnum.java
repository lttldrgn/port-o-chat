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

/**
 *
 * @author Brandon
 */
public enum ServerMessageEnum {
    USERNAME_SET(0, "Username set to"),
    ERROR_USERNAME_IN_USE(1, "Username in use"),
    ERROR_CHANNEL_NON_EXISTENT(2, "Can't send message to a non-existant channel"),
    ERROR_USER_NON_EXISTENT(3, "Can't send message to a non-existant user"),
    ERROR_NO_USERNAME(4, "You must first send a username!");

    private int value;
    private String message;
    
    ServerMessageEnum(int value, String message) {
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
    
    
}