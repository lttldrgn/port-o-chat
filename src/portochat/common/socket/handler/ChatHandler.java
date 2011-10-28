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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import portochat.common.User;
import portochat.common.encryption.EncryptionManager;
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
    private ProtocolHandler protocolHandler = null;
    private EncryptionManager encryptionManager = null;
    private Boolean encrypted = null;
    
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
        
        byte[] parseBuffer = Arrays.copyOf(buffer, length);
        if (isStreamEncrypted(socket)) {
            parseBuffer = encryptionManager.decrypt(getSecretKey(socket), parseBuffer);
            length = parseBuffer.length;
        }
        listenerDataList = protocolHandler.processData(parseBuffer, length);
        
        return (!error);
    }
    
    @Override
    public byte[] processOutgoing(Socket socket, byte[] buffer, int length) {
        logger.log(Level.FINEST, "ChatHandler.processOutgoing");

        byte[] parseBuffer = Arrays.copyOf(buffer, length);
        if (isStreamEncrypted(socket)) {
            parseBuffer = encryptionManager.encrypt(getSecretKey(socket), parseBuffer);
        }
        
        return parseBuffer;
    }
    
    private boolean isStreamEncrypted(Socket socket) {
        
        if (encrypted == null) {
            if (serverHandler) {
                User user = UserDatabase.getInstance().getSocketOfUser(socket);
                encrypted = (user.getSecretKey() != null);
            } else {
                encrypted = (encryptionManager.getServerSecretKey() != null);
            }
        }
        
        return encrypted;
    }
    
    private SecretKey getSecretKey(Socket socket) {
        SecretKey secretKey = null;
    
        if (serverHandler) {
            User user = UserDatabase.getInstance().getSocketOfUser(socket);
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
