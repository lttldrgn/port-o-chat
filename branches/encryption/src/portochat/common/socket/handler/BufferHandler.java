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
package portochat.common.socket.handler;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import portochat.common.protocol.DefaultData;

/**
 * This is the abstract class for handling buffers. The idea behind buffer 
 * handlers is to provide a sequential/parallel way to processIncoming messages. These
 * handlers should be used as such:
 * 
 * 1.) A list of handlers is presented to the server/client. 
 * 2.) When reading the incoming buffer, an iteration of the handlers is done
 * 2a.) If the chosen handler consumes the message, iteration stops.
 * 2b.) If the chosen handler is finished, the handler is removed from the list.
 * 
 * @author Mike
 */
public abstract class BufferHandler {
    protected boolean messageConsumed = false;
    protected boolean finished = false;
    protected boolean serverHandler = false;
    protected static Map<String, Object> globalMap = null;
    protected List<DefaultData> listenerDataList = null;
    protected List<DefaultData> socketDataList = null;
    
    static {
        globalMap = new ConcurrentHashMap<String, Object>();
    }
    
    /**
     * Constructor
     */
    public BufferHandler() {
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
     * @param socket The socket the buffer is sending to
     * @param buffer The buffer to process
     * @param length The readable length of the buffer
     * @return the byte array that should be sent to the socket
     */
    public abstract byte[] processOutgoing (Socket socket, byte buffer[], int length);
    
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
    
    public static Object getGlobal(String key) {
        return globalMap.get(key);
    }

    public static void putGlobal(String key, Object value) {
        globalMap.put(key, value);
    }

    public List<DefaultData> getListenerData() {
        return listenerDataList;
    }
    
    public List<DefaultData> getSocketData() {
        return socketDataList;
    }

    public boolean isServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(boolean serverHandler) {
        this.serverHandler = serverHandler;
    }
    
}
