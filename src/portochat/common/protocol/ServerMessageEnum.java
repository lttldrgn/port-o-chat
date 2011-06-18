/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

/**
 *
 * @author Brandon
 */
public enum ServerMessageEnum {
    USER_SET(0, "User set to"),
    ERROR_USERNAME_IN_USE(1, "Username in use"),
    ERROR_CHANNEL_NON_EXISTENT(2, "Can't send message to a non-existant channel"),
    ERROR_USER_NON_EXISTENT(2, "Can't send message to a non-existant user"),
    ERROR_NO_USERNAME(3, "You must first send a username!");

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