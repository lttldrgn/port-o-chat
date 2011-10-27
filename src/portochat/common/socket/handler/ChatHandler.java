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
import javax.crypto.SecretKey;
import portochat.common.User;
import portochat.common.encryption.EncryptionManager;
import portochat.common.protocol.ProtocolHandler;
import portochat.server.UserDatabase;

/**
 *
 * @author Mike
 */
public class ChatHandler extends BufferHandler {

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

        System.out.println("ChatHandler.processIncoming");
        
        // if encryption is on
        // decrypt
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
        System.out.println("ChatHandler.processOutgoing");
        /*
        if (serverHandler) {
            User user = UserDatabase.getInstance().getSocketOfUser(socket);
            System.out.println("server's serverSecretKey: " + user.getSecretKey());
            System.out.println("user's clientPublicKey: " + user.getClientPublicKey());
        } else {
            System.out.println("client's serverSecretKey: " + encryptionManager.getServerSecretKey());
            System.out.println("client's clientPublicKey: " + encryptionManager.getClientPublicKey());
            System.out.println("client's clientPrivateKey: " + encryptionManager.getClientPrivateKey());
        }*/
        
        // if encryption on
        // encrypt
        byte[] parseBuffer = Arrays.copyOf(buffer, length);
        if (isStreamEncrypted(socket)) {
            parseBuffer = encryptionManager.encrypt(getSecretKey(socket), parseBuffer);
        }
        
        return parseBuffer;
    }
    
    private boolean isStreamEncrypted(Socket socket) {
        
        if (encrypted == null) {
            // TODO another way to do this?
            // how can I get clients to view this data as well?
            // global map won't work if user hosts server and client
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
}
