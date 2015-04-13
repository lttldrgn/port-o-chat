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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import portochat.common.User;
import portochat.common.Util;
import portochat.common.encryption.EncryptionManager;
import portochat.common.network.ConnectionHandler.NetData;
import portochat.common.protocol.DefaultData;
import portochat.common.protocol.Initialization;
import portochat.common.protocol.InitializationEnum;
import portochat.common.protocol.ProtocolHandler;
import portochat.server.UserDatabase;

/**
 * The purpose of this class is to process handshake events. The events occur
 * as follows:
 * 
 * 1.) Client connects
 * 2.) Client sends initialization data containing public RSA key
 * 3.) Server responds with encryption on or off
 * 3a.) If encryption is on, the client's RSA key is saved and the
 *      server also sends the secret key encoded w/ the client's public RSA key.
 * 4.) The client receives encryption on or off
 * 4a.) If the client sees encryption off, the client responds with a READY.
 * 4b.) If the client sees encryption on, the client decodes the encrypted secret
 *      key using the private RSA key, and then responds with a READY.
 * 5.) The server receives the client's READY and responds with it's own READY
 * 6.) The client sees the server's READY and notifies that chat can begin.\
 * 
 * @author Mike
 */
public class HandshakeHandler extends BufferHandler {

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

        logger.log(Level.FINEST, "HandshakeHandler.processIncoming");

        List<DefaultData> tempDefaultDataList =
                protocolHandler.processData(buffer, length);

        if (tempDefaultDataList != null
                && tempDefaultDataList.size() > 0) {
            for (DefaultData data : tempDefaultDataList) {
                if (data instanceof Initialization) {
                    handleInitializationHandshake(socket, (Initialization) data);
                }
            }
        }

        return (!error);
    }

    @Override
    public byte[] processOutgoing(NetData data) {
        logger.log(Level.FINEST, "HandshakeHandler.processOutgoing");
        return Arrays.copyOf(data.data, data.data.length);
    }

    private void handleInitializationHandshake(Socket socket, Initialization init) {

        InitializationEnum initEnum =
                init.getInitializationEnum();
        logger.log(Level.FINEST, "initEnum: {0}", initEnum);

        if (serverHandler) {
            // We are a server
            if (userDatabase == null) {
                userDatabase = UserDatabase.getInstance();
            }

            User user = userDatabase.getUserOfSocket(socket);
            user.setInitEnum(initEnum);

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

                        user.setClientPublicKey(encryptionManager.getClientPublicKey(init.getEncodedPublicKey()));
                        logger.log(Level.FINEST, "public: {0}",
                                Util.byteArrayToHexString(user.getClientPublicKey().getEncoded()));

                        // Encode the private key using the client's public key
                        byte[] encodedEncryptedSecretKey =
                                encryptionManager.encryptSecretKeyWithPublicKey(
                                user.getSecretKey(), user.getClientPublicKey());
                        logger.log(Level.FINEST, "encodedEncryptedSecretKey: {0}",
                                Util.byteArrayToHexString(encodedEncryptedSecretKey));

                        // Set the encoded encrypted secret key
                        newInit.setEncodedEncryptedSecretKey(encodedEncryptedSecretKey);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE,
                                "Unable to encode AES key using client's public RSA key", ex);
                        return;
                    }
                }

                // Send the message
                socketDataList.clear();
                socketDataList.add(newInit);
            } else if (initEnum == InitializationEnum.READY) {

                logger.log(Level.FINEST,
                        "The client says they're ready, telling client we're ready too");

                // Set that we're ready
                Initialization newInit = new Initialization();
                newInit.setServer(true);
                newInit.setInitializationEnum(InitializationEnum.READY);

                // Send the message
                socketDataList.clear();
                socketDataList.add(newInit);

                // Set this handler as finished
                finished = true;
            } else {
                // Unknown enum
                logger.log(Level.WARNING,
                        "Client sent unknown initialization state: {0}",
                        initEnum);
                socketDataList.clear();
                listenerDataList.clear();
            }
        } else {
            // We are a client

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
                socketDataList.clear();
                socketDataList.add(newInit);

            } else if (initEnum == InitializationEnum.ENCRYPTION_OFF) {
                encryption = false;

                // Set the client as ready
                Initialization newInit = new Initialization();
                newInit.setInitializationEnum(InitializationEnum.READY);

                // Send the message
                socketDataList.clear();
                socketDataList.add(newInit);
            } else if (initEnum == InitializationEnum.READY) {
                logger.log(Level.FINEST, 
                        "The server said they're ready too, notify the client!");

                // Notify the client we're ready
                listenerDataList.clear();
                listenerDataList.add(init);

                // clear socket data
                socketDataList.clear();

                // Set this handler as finished
                finished = true;
            } else {
                // Unknown enum
                logger.log(Level.WARNING,
                        "Received unknown initalization state from server: {0}",
                        initEnum);
                socketDataList.clear();
                listenerDataList.clear();
            }
        }
    }

    public boolean getEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    /**
     * @return Retrieves the name of the handler.
     */
    @Override
    public String getName() {
        return "HandshakeHandler";
    }
}
