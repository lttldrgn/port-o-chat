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
package portochat.common.network.handler;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import portochat.common.User;
import portochat.common.encryption.EncryptionManager;
import portochat.common.network.ConnectionHandler.NetData;
import portochat.common.protocol.ProtocolHandler;
import portochat.server.UserDatabase;

/**
 * The purpose of this class is to process chat messages. Note that these
 * messages can be encrypted.
 
 * @author Mike
 */
public class ChatHandler extends BufferHandler {

    private static final Logger logger =
            Logger.getLogger(BufferHandler.class.getName());
    private final ProtocolHandler protocolHandler;
    private final EncryptionManager encryptionManager;
    
    public ChatHandler() {
        super();
        
        protocolHandler = ProtocolHandler.getInstance();
        encryptionManager = EncryptionManager.getInstance();
        messageConsumed = false;
    }
    
    @Override
    public boolean processIncoming(Socket socket, byte[] buffer, int length) {
        boolean error = false;

        logger.log(Level.FINEST, "ChatHandler.processOutgoing");
        int newLength = length-1;
        byte[] parseBuffer = new byte[newLength];
        System.arraycopy(buffer, 1, parseBuffer, 0, newLength);
        if (buffer[0] == 1 && isEncryptionEnabled(socket)) {
            parseBuffer = encryptionManager.decrypt(getSecretKey(socket), parseBuffer);
            newLength = parseBuffer.length;
        }
        listenerDataList = protocolHandler.processData(parseBuffer, newLength);
        
        return (!error);
    }
    
    @Override
    public byte[] processOutgoing(NetData data) {
        logger.log(Level.FINEST, "ChatHandler.processOutgoing");
        Socket socket = data.socket;
        byte[] buffer = data.data;

        // create an output buffer with the first byte indicating encryption
        byte[] outputBuffer;
        
        if (data.canBeEncrypted && isEncryptionEnabled(socket)) {
            byte encrypted[] = encryptionManager.encrypt(getSecretKey(socket), buffer);
            outputBuffer = new byte[encrypted.length + 1];
            outputBuffer[0] = 1; // encryption
            System.arraycopy(encrypted, 0, outputBuffer, 1, encrypted.length);
        } else {
            // use original unmodified buffer
            outputBuffer = new byte[buffer.length+1];
            outputBuffer[0] = 0; // no encryption
            System.arraycopy(buffer, 0, outputBuffer, 1, buffer.length);
        }
        
        return outputBuffer;
    }
    
    private boolean isEncryptionEnabled(Socket socket) {
        
        boolean encrypted;
        if (serverHandler) {
            encrypted = UserDatabase.getInstance().isSocketEncrypted(socket);
        } else {
            encrypted = (encryptionManager.getServerSecretKey() != null);
        }
        
        return encrypted;
    }
    
    private SecretKey getSecretKey(Socket socket) {
        SecretKey secretKey;
    
        if (serverHandler) {
            User user = UserDatabase.getInstance().getUserOfSocket(socket);
            secretKey = user.getSecretKey();
        } else {
            secretKey = encryptionManager.getServerSecretKey();
        }

        return secretKey;
    }
    
    @Override
    public String getName() {
        return "ChatHandler";
    }
}
