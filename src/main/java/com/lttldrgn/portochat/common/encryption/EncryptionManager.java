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
package com.lttldrgn.portochat.common.encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.lttldrgn.portochat.common.Util;

/**
 * This class is used to generate encryption keys, encode, and decode.
 * Also, clients can store their keys in here as well for future use.
 * 
 * @author Mike
 */
public class EncryptionManager {

    private static final Logger logger =
            Logger.getLogger(Logger.class.getName());
    private static EncryptionManager instance = null;
    private SecretKey serverSecretKey = null; // used for clients to store in
    private KeyPair clientKeyPair = null;
    private final boolean DEBUG = false;
    
    /**
     * Private constructor
     */
    private EncryptionManager() {
        if (DEBUG) {
            logger.setLevel(Level.ALL);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            logger.addHandler(consoleHandler);
        }
    }

    /**
     * @return The singleton instance of the EncryptionManager
     */
    public synchronized static EncryptionManager getInstance() {
        if (instance == null) {
            instance = new EncryptionManager();
        }

        return instance;
    }

    /**
     * This is used by the server to generate a secret key. The secret key
     * is used by both the client and server to encode/decode with.
     * 
     * @return The secret key
     */
    public SecretKey generateServerSecretKey() {

        SecretKey secretKey = null;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128); // 192 and 256 bits may not be available
            secretKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "Unable to generate server secret key!", ex);
        }

        return secretKey;
    }

    /**
     * Encrypts a byte array encryption
     * 
     * @param secretKey Key to encrypt message
     * @param data The byte array to encrypt
     * @return The encrypted byte array (can be null if an error occurred)
     */
    public byte[] encrypt(SecretKey secretKey, byte[] data) {
        byte[] ivCipherData = null;
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "\nencrypt(data) ->" + "\ndata:{0}",
                    Util.byteArrayToHexString(data));
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedByteArray = cipher.doFinal(data);

            //TODO is this random?
            IvParameterSpec iv =
                    cipher.getParameters().
                    getParameterSpec(IvParameterSpec.class);

            // Combine
            byte[] ivBytes = iv.getIV();

            EncryptedData encryptedData = new EncryptedData();
            encryptedData.setIvBytes(ivBytes);
            encryptedData.setEncryptedByteArray(encryptedByteArray);
            ivCipherData = encryptedData.getByteArray();
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "encryptedData[{0}]:{1}", 
                    new Object[]{
                        encryptedData.getByteArray().length,
                        Util.byteArrayToHexString(encryptedData.getByteArray())});
                
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | 
                IllegalBlockSizeException | BadPaddingException | InvalidParameterSpecException ex) {
            logger.log(Level.SEVERE, "Unable to encrypt message!", ex);
        }

        return ivCipherData;
    }

    /**
     * Decrypts the byte array
     * 
     * @param secretKey Key to decode message
     * @param encryptedBytes The encrypted bytes to be decrypted
     * @return The decrypted byte array (can be null if an error occurred)
     */
    public byte[] decrypt(SecretKey secretKey, byte[] encryptedBytes) {
        byte[] data = new byte[0];

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST,"\ndecrypt(encryptedData) ->"
                    + "\nencryptedData:[{0}]:{1}", 
                    new Object[]{
                        encryptedBytes.length,
                        Util.byteArrayToHexString(encryptedBytes)});
        }
        
        try {
            int remainingBytes = encryptedBytes.length;
            while (remainingBytes > 0) {
                EncryptedData encryptedData = new EncryptedData();
                encryptedData.setData(encryptedBytes);

                // Get the remaining bytes to be decoded
                remainingBytes -= encryptedData.getParsedLength();
                byte[] remainingEncryptedBytes = new byte[remainingBytes];
                System.arraycopy(encryptedBytes, encryptedData.getParsedLength(), 
                        remainingEncryptedBytes, 0, remainingBytes);
                encryptedBytes = remainingEncryptedBytes;
                
                IvParameterSpec iv = new IvParameterSpec(encryptedData.getIvBytes());
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
                data = Util.concat(data, 
                        cipher.doFinal(encryptedData.getEncodedData()));                
            }
            
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | 
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            logger.log(Level.SEVERE, "Unable to decrypt data", ex);
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "data:{0}", Util.byteArrayToHexString(data));
        }

        return data;
    }

    /**
     * Client's use this method to store the secret key received from the server.
     * 
     * @param serverSecretKey The server's secret key
     */
    public void setServerSecretKey(SecretKey serverSecretKey) {
        this.serverSecretKey = serverSecretKey;
    }

    /**
     * Client's use this method to retrieve their secret key, which is used
     * to talk to the server.
     * 
     * @return The server's secret key
     */
    public SecretKey getServerSecretKey() {
        return serverSecretKey;
    }

    /**
     * Generates a client key
     * 
     * @throws NoSuchAlgorithmException 
     */
    private void generateClientKey()
            throws NoSuchAlgorithmException {
        if (clientKeyPair == null) {
            // Generate a key
            KeyPairGenerator keyGen;
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            clientKeyPair = keyGen.generateKeyPair();
        }
    }

    /**
     * Gets the client's encoded public key. This is used to transfer the
     * client's public key over the network.
     * 
     * @return the byte array of the client's public key
     */
    public byte[] getClientEncodedPublicKey() {
        PublicKey publicKey = getClientPublicKey();
       
        return (publicKey != null?publicKey.getEncoded():null);
    }

    /**
     * Gets the client's public key. The client's public key is used by
     * the server to encode messages sent to the client.

     * @return The client's public key
     */
    public PublicKey getClientPublicKey() {
        PublicKey publicKey = null;

        try {
            if (clientKeyPair == null) {
                generateClientKey();
            }
            publicKey = clientKeyPair.getPublic();
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "No such algorithm", ex);
        }

        return publicKey;
    }

    /**
     * This method takes in the encoded public key, and returns the public key
     * of the client.
     * 
     * @param encodedClientPublicKey The byte array representing the client's
     *          public key.
     * 
     * @return The client's public key
     */
    public PublicKey getClientPublicKey(byte[] encodedClientPublicKey) {

        PublicKey publicKey = null;

        try {
            X509EncodedKeySpec publicKeySpec =
                    new X509EncodedKeySpec(encodedClientPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            logger.log(Level.SEVERE, "Unable to generate client's public key "
                    + "from the encoded byte array!", ex);
        }

        return publicKey;
    }

    /**
     * This is used by the client to decode the messages from the server which
     * are encoded with the public key.
     * 
     * @return The private key
     */
    public PrivateKey getClientPrivateKey() {
        PrivateKey privateKey = null;

        try {
            if (clientKeyPair == null) {
                generateClientKey();
            }
            privateKey = clientKeyPair.getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "No such algorithm", ex);
        }

        return privateKey;
    }

    /**
     * This method is used by the server to encrypt the server's secret 
     * key with the client's public key. This is a safe way to transfer 
     * the server's secret key.
     * 
     * @param serverSecretKey The server's secret key.
     * @param clientPublicKey The client's public key.
     
     * @return The encrypted secret key using the public key.
     */
    public byte[] encryptSecretKeyWithPublicKey(
            SecretKey serverSecretKey,
            PublicKey clientPublicKey) {

        byte[] encodedEncryptedSecretKey = null;

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
            encodedEncryptedSecretKey =
                    cipher.doFinal(serverSecretKey.getEncoded());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | 
                IllegalBlockSizeException | BadPaddingException ex) {
            logger.log(Level.SEVERE, "Unable to encrypt secret key with "
                    + "public key!", ex);
        }

        return encodedEncryptedSecretKey;
    }

    /**
     * This method is used by the client to decode the server's secret key
     * using the client's private key. The secret key will be used to 
     * encrypt and decrypt messages from the client and server.
     * 
     * @param encodedEncryptedSecretKey The encrypted secret key from the server.

     * @return The secret key
     */
    public SecretKey decodeSecretKeyWithPrivateKey(byte[] encodedEncryptedSecretKey) {

        SecretKey secretKey = null;

        try {
            Cipher privateCipher = Cipher.getInstance("RSA");
            privateCipher.init(Cipher.DECRYPT_MODE, getClientPrivateKey());
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "encodedEncryptedSecretKey: {0}",
                        Util.byteArrayToHexString(encodedEncryptedSecretKey));
            }
            
            byte[] encodedSecretKey =
                    privateCipher.doFinal(encodedEncryptedSecretKey);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "encodedSecretKey: {0}", 
                        Util.byteArrayToHexString(encodedSecretKey));
            }
            secretKey = new SecretKeySpec(encodedSecretKey, "AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | 
                IllegalBlockSizeException | BadPaddingException ex) {
            logger.log(Level.SEVERE, "Unable to decode secret key with "
                    + "private key!", ex);
        }

        return secretKey;
    }

    /**
     * Private class for containing the encrypted data.
     */
    private class EncryptedData {

        private byte[] ivBytes = null;
        private byte[] encodedData = null;
        int parsedLength = -1;
        
        private EncryptedData() {
        }

        private byte[] getByteArray() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            try {
                dos.writeInt(ivBytes.length);
                dos.write(ivBytes);
                dos.writeInt(encodedData.length);
                dos.write(encodedData);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to create EncryptedData", ex);
            }
            return baos.toByteArray();
        }

        private void setData(byte[] data) {

            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));

            try {
                int ivBytesLength = dis.readInt();
                ivBytes = new byte[ivBytesLength];
                dis.read(ivBytes);
                int encodedDataLength = dis.readInt();
                encodedData = new byte[encodedDataLength];
                dis.read(encodedData);
                parsedLength = 4/*ivBytesLength*/ + ivBytesLength + 
                        4/*encodedDataLength*/ + encodedDataLength;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to set data from EncryptedData", ex);
            }
        }

        private byte[] getEncodedData() {
            return encodedData;
        }

        private void setEncryptedByteArray(byte[] encodedData) {
            this.encodedData = encodedData;
        }

        private byte[] getIvBytes() {
            return ivBytes;
        }

        private void setIvBytes(byte[] ivBytes) {
            this.ivBytes = ivBytes;
        }

        public int getParsedLength() {
            return parsedLength;
        }
    }
}
