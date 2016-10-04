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
package com.lttldrgn.portochat.common.network.handler;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.lttldrgn.portochat.common.network.ConnectionHandler.NetData;
import com.lttldrgn.portochat.common.protocol.DefaultData;

/**
 * This is the abstract class for handling buffers. The idea behind buffer 
 * handlers is to provide a sequential/parallel way to processIncoming messages. These
 * handlers should be used as such:
 * 
 * 1.) A list of handlers is presented to the server/client. 
 * 2.) When reading the incoming buffer, an iteration of the handlers is done.
 * 2a.) If the chosen handler is finished, the handler skipped.
 * 2b.) If the chosen handler consumes the message, iteration stops.
 * 3.) When writing the outgoing buffer, an iteration of the handlers is done.
 * 3a.) If the chosen handler is finished, the handler is removed from the list.
 * 3b.) If the chosen handler consumes the message, iteration stops.
  * 
 * @author Mike
 */
public abstract class BufferHandler {
    protected boolean messageConsumed = false;
    protected boolean finished = false;
    protected boolean serverHandler = false;
    protected List<DefaultData> listenerDataList = null;
    protected List<DefaultData> socketDataList = null;
    
    /**
     * Constructor
     */
    public BufferHandler() {
        listenerDataList = new CopyOnWriteArrayList<>();
        socketDataList = new CopyOnWriteArrayList<>();
    }
    
    /**
     * This method processes the received buffer.
     * 
     * @param socket The socket the buffer was received from
     * @param buffer The buffer to process
     * @param length The readable length of the buffer
     * @return true if successful
     */
    public abstract boolean processIncoming (Socket socket, byte buffer[], int length);
  
    /**
     * This method processes the outgoing buffer.
     * 
     * @param netData Original packet data
     * @return the byte array that should be sent to the socket
     */
    public abstract byte[] processOutgoing (NetData netData);
    
    /**
     * @return true if this handler consumes the message.
     */
    public boolean isMessageConsumed() {
        return messageConsumed;
    }

    /**
     * @return true if this handler is finished processing all messages.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * This retrieves any messages which should be broadcasted out to
     * registered listeners in TCPSocket
     * 
     * @return The list of data that should be fired to registered listeners
     */
    public List<DefaultData> getListenerData() {
        return listenerDataList;
    }
    
    /**
     * This retrieves any messages which should be written out in the
     * socket buffer.
     * 
     * @return The list of data which should be written out in the socket 
     *  buffer.
     */
    public List<DefaultData> getSocketData() {
        return socketDataList;
    }

    /**
     * @return true if the handler is ran on a server
     */
    public boolean isServerHandler() {
        return serverHandler;
    }

    /**
     * Sets if the handler is ran on a server.
     * 
     * @param serverHandler true if the handler is ran on a server.
     */
    public void setServerHandler(boolean serverHandler) {
        this.serverHandler = serverHandler;
    }
    
    /**
     * @return Retrieves the name of the handler.
     */
    public abstract String getName();
}
