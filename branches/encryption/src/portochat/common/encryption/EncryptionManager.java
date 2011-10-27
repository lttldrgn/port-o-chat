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
package portochat.common.encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import portochat.common.Util;

/**
 *
 * @author Mike
 */
public class EncryptionManager {

    private static final Logger logger =
            Logger.getLogger(Logger.class.getName());
    private static EncryptionManager instance = null;
    private SecretKey serverSecretKey = null; // used for clients to store in
    private KeyPair clientKeyPair = null;

    private EncryptionManager() {
    }

    public synchronized static EncryptionManager getInstance() {
        if (instance == null) {
            instance = new EncryptionManager();
        }

        return instance;
    }

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
     * @param node TCPnode of the connection
     * @param data The byte array to encrypt
     * @return The encrypted byte array (can be null if an error occurred)
     */
    public byte[] encrypt(SecretKey secretKey, byte[] data) {
        EncryptedData encryptedData = null;

        System.out.println("\nencrypt(data) ->"
                + "\ndata:" + Util.byteArrayToHexString(data));

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

            encryptedData = new EncryptedData();
            encryptedData.setIvBytes(ivBytes);
            encryptedData.setEncryptedByteArray(encryptedByteArray);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to encrypt message!", ex);
        }

        System.out.println("encryptedData:"
                + Util.byteArrayToHexString(encryptedData.getByteArray()));

        return encryptedData.getByteArray();
    }

    /**
     * Decrypts the byte array
     * 
     * @param node TCPNode of the connection
     * @param encryptedBytes The encrypted bytes to be decrypted
     * @return The decrypted byte array (can be null if an error occurred)
     */
    public byte[] decrypt(SecretKey secretKey, byte[] encryptedBytes) {
        byte[] data = null;

        System.out.println("\ndecrypt(encryptedData) ->"
                + "\nencryptedData:"
                + Util.byteArrayToHexString(encryptedBytes));
        try {
            EncryptedData encryptedData = new EncryptedData();
            encryptedData.setData(encryptedBytes);
            IvParameterSpec iv = new IvParameterSpec(encryptedData.ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            data = cipher.doFinal(encryptedData.encodedData);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to decrypt data", ex);
        }

        System.out.println("data:" + Util.byteArrayToHexString(data));

        return data;
    }

    public void setServerSecretKey(SecretKey secretKey) {
        serverSecretKey = secretKey;
    }

    public SecretKey getServerSecretKey() {
        return serverSecretKey;
    }

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

    public byte[] getClientEncodedPublicKey() {
        byte[] encodedPublicKey = null;

        try {
            if (clientKeyPair == null) {
                generateClientKey();
            }
            encodedPublicKey = clientKeyPair.getPublic().getEncoded();
            System.out.println("public: "
                    + Util.byteArrayToHexString(
                    clientKeyPair.getPublic().getEncoded()));
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "No such algorithm", ex);
        }

        return encodedPublicKey;
    }

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

    public PublicKey getClientPublicKey(byte[] encodedClientPublicKey) {

        PublicKey publicKey = null;

        try {
            X509EncodedKeySpec publicKeySpec =
                    new X509EncodedKeySpec(encodedClientPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to generate client's public key "
                    + "from the encoded byte array!", ex);
        }

        return publicKey;
    }

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

    public byte[] encryptSecretKeyWithPublicKey(
            SecretKey serverSecretKey,
            PublicKey clientPublicKey) {

        byte[] encodedEncryptedSecretKey = null;

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
            encodedEncryptedSecretKey =
                    cipher.doFinal(serverSecretKey.getEncoded());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to encrypt secret key with "
                    + "public key!", ex);
        }

        return encodedEncryptedSecretKey;
    }

    public SecretKey decodeSecretKeyWithPrivateKey(byte[] encodedEncryptedSecretKey) {

        SecretKey serverSecretKey = null;

        try {
            Cipher privateCipher = Cipher.getInstance("RSA");
            privateCipher.init(Cipher.DECRYPT_MODE, getClientPrivateKey());
            System.out.println("encodedEncryptedSecretKey: " + Util.byteArrayToHexString(encodedEncryptedSecretKey));
            byte[] encodedSecretKey =
                    privateCipher.doFinal(encodedEncryptedSecretKey);
            System.out.println("encodedSecretKey: " + Util.byteArrayToHexString(encodedSecretKey));
            serverSecretKey = new SecretKeySpec(encodedSecretKey, "AES");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to decode secret key with "
                    + "private key!", ex);
        }

        return serverSecretKey;
    }

    private class EncryptedData {

        private byte[] ivBytes = null;
        private byte[] encodedData = null;

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
    }
}
