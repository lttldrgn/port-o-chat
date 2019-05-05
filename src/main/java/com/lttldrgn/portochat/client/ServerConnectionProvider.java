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
package com.lttldrgn.portochat.client;

/**
 * This interface defines methods to access the server and should be implemented
 * by an object that can forward information through a ServerConnection
 * 
 */
public interface ServerConnectionProvider {
    /**
     * Sends a message to the defined recipient
     * @param recipient Person or channel the message is being sent to
     * @param isChannel Set true if the destination is a channel
     * @param action True if this is an action message
     * @param message Message being sent
     * @return True if the message was attempted to be sent. False if not connected.
     */
    public boolean sendMessage(String recipient, boolean isChannel, boolean action, String message);
    
    /**
     * Returns the name that the client is connected as on the server
     * @return Username that is set on the server
     */
    public String getConnectedUsername();
}
