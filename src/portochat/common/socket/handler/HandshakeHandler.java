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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import portochat.common.User;
import portochat.common.Util;
import portochat.common.encryption.EncryptionManager;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Initialization;
import portochat.common.protocol.InitializationEnum;
import portochat.common.protocol.ProtocolHandler;
import portochat.server.UserDatabase;

/**
 *
 * @author Mike
 */
public class HandshakeHandler extends BufferHandler {

    //TODO somehow client starts by doing initializeConnectingNode from old tcpsocket.
    // maybe we start w/ enum null then we know it's the first enum and start?
    // after that we can chain to 2-3-4-5?
    // probably need to send the node/server w/ buffer? this way we put in global
    // map so we can decode later on
    private static final Logger logger = 
            Logger.getLogger(HandshakeHandler.class.getName());
    private ProtocolHandler protocolHandler = null;
    private boolean encryption = false;
    private EncryptionManager encryptionManager = null;
    private UserDatabase userDatabase = null;
    
    public HandshakeHandler() {
        super();

        protocolHandler = ProtocolHandler.getInstance();
        encryptionManager = EncryptionManager.getInstance();
        messageConsumed = true;
    }

    @Override
    public boolean processIncoming(Socket socket, byte[] buffer, int length) {
        boolean error = false;

        System.out.println("HandshakeHandler.processIncoming");
        
        List<DefaultData> tempDefaultDataList =
                protocolHandler.processData(buffer, length);
        
        if (tempDefaultDataList != null 
                && tempDefaultDataList.size() > 0) {
            for (DefaultData data : tempDefaultDataList) {
                if (data instanceof Initialization) {
                    System.out.println(data);
                    handleInitializationHandshake(socket, (Initialization)data);
                }
            }
        }
        
        return (!error);
    }
    
    @Override
    public byte[] processOutgoing(Socket socket, byte[] buffer, int length) {
        System.out.println("HandshakeHandler.processOutgoing");
        return Arrays.copyOf(buffer, length);
    }
    
    private void handleInitializationHandshake(Socket socket, Initialization init) {
        
        if (serverHandler) {
            // We are a server
            System.out.println("Server");
            
            if (userDatabase == null) {
                userDatabase = UserDatabase.getInstance();
            }
            
            User user = userDatabase.getSocketOfUser(socket);
            
            InitializationEnum initEnum = 
                    init.getInitializationEnum();
            user.setInitEnum(initEnum);
System.out.println("initEnum: " + initEnum);

            if (initEnum == InitializationEnum.CLIENT_RSA_PRIVATE_KEY) {

                Initialization newInit = new Initialization();
                newInit.setServer(true);
                newInit.setInitializationEnum(InitializationEnum.ENCRYPTION_OFF);
                
                if (encryption) {
                    newInit.setInitializationEnum(InitializationEnum.ENCRYPTION_ON);
                    
                    try {
                        // Generate secret key for user
                        user.setSecretKey(
                                encryptionManager.generateServerSecretKey());

                        user.setClientPublicKey(encryptionManager.
                                getClientPublicKey(init.getEncodedPublicKey()));
System.out.println("public: " + Util.byteArrayToHexString(user.getClientPublicKey().getEncoded()));
                        // Encode the private key using the client's public key
                        byte[] encodedEncryptedSecretKey = 
                                encryptionManager.encryptSecretKeyWithPublicKey(
                                    user.getSecretKey(), user.getClientPublicKey());

System.out.println("encodedEncryptedSecretKey: " + Util.byteArrayToHexString(encodedEncryptedSecretKey));

                        // Set the encoded encrypted secret key
                        newInit.setEncodedEncryptedSecretKey(encodedEncryptedSecretKey);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, 
                                "Unable to encode AES key using client's public RSA key", ex);
                        //destroy(node);
                        return;
                    }
                }
                
                // Send the message
                socketDataList = new ArrayList<DefaultData>();
                socketDataList.add(newInit);
            } else if (initEnum == InitializationEnum.READY) {
                
System.out.println("The client says they're ready, telling client we're ready too");
                // Set that we're ready
                Initialization newInit = new Initialization();
                newInit.setServer(true);
                newInit.setInitializationEnum(InitializationEnum.READY);

                // Send the message
                // TODO Why does setting this screw the client up?
                // Need to figure out how to tell the client to send it's username
                // commented out client.java:421 for that
                //writeData(node.socket, node.init);
                socketDataList = new ArrayList<DefaultData>();
                socketDataList.add(newInit);
                
                // Set this handler as finished
                finished = true;
            } else {
                // Unknown enum, disconnect the node
                logger.log(Level.INFO,
                        "Disconnecting node since it sent an unknown " +
                        "initialization state: {0}", initEnum);
                //destroy(node);
            }
        } else {
            // We are a client
            System.out.println("client");
            InitializationEnum initEnum =
                    init.getInitializationEnum();
            System.out.println("initEnum: " + initEnum);
            
            if (initEnum == InitializationEnum.ENCRYPTION_ON) {

                // Decode the AES key using the client's private key
                SecretKey serverSecretKey = 
                        encryptionManager.decodeSecretKeyWithPrivateKey(
                            init.getEncodedEncryptedSecretKey());
                
                encryptionManager.setServerSecretKey(serverSecretKey);
                encryption = true;

                // Set the client as ready
                Initialization newInit = new Initialization();
                newInit.setInitializationEnum(InitializationEnum.READY);

                // Send the message
                socketDataList = new ArrayList<DefaultData>();
                socketDataList.add(newInit);  

            } else if (initEnum == InitializationEnum.ENCRYPTION_OFF) {
                encryption = false;
System.out.println("Client sending we're ready!");
                // Set the client as ready
                Initialization newInit = new Initialization();
                newInit.setInitializationEnum(InitializationEnum.READY);
                
                // Send the message
                socketDataList = new ArrayList<DefaultData>();
                socketDataList.add(newInit);               
            } else if (initEnum == InitializationEnum.READY) {
                System.out.println("The server said they're ready too, notify someone!");

                // Notify the client we're ready
                listenerDataList = new ArrayList<DefaultData>();
                listenerDataList.add(init);
                
                // Set this handler as finished
                finished = true;
            } else {
                // Unknown enum, disconnect from the server
                logger.log(Level.INFO,
                        "Disconnecting from server we received an unknown " +
                        "initialization state: {0}", initEnum);
                //disconnect();
                return;
            }
        }
    }
    
    public boolean getEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

}
