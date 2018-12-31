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
package com.lttldrgn.portochat.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles parsing byte arrays into DefaultData objects which can be
 * used to process information.
 * 
 * @author Mike
 */
public class ProtocolHandler {

    private Map<Byte, String> protocolClassMap = null;
    private Map<String, Byte> protocolHeaderMap = null;
    private static ProtocolHandler instance = null;
    private static final Logger logger = Logger.getLogger(ProtocolHandler.class.getName());
    
    /**
     * Private constructor
     */
    private ProtocolHandler() {
    }

    /**
     * Method to get the instance of this singleton.
     * 
     * @return ProtocolHandler
     */
    public static ProtocolHandler getInstance() {
        if (instance == null) {
            instance = new ProtocolHandler();
            instance.initialize();
        }

        return instance;
    }

    /**
     * Initializes the protocol handler
     */
    private void initialize() {
        protocolClassMap = new HashMap<>();
        protocolClassMap.put((byte)0, "com.lttldrgn.portochat.common.protocol.ProtoMessage");
        protocolClassMap.put((byte)4, "com.lttldrgn.portochat.common.protocol.ServerMessage");
        protocolClassMap.put((byte)5, "com.lttldrgn.portochat.common.protocol.Ping");
        protocolClassMap.put((byte)6, "com.lttldrgn.portochat.common.protocol.Pong");
        protocolClassMap.put((byte)7, "com.lttldrgn.portochat.common.protocol.UserConnectionStatus");
        protocolClassMap.put((byte)8, "com.lttldrgn.portochat.common.protocol.SetPublicKey");
        protocolClassMap.put((byte)9, "com.lttldrgn.portochat.common.protocol.ServerSharedKey");
        protocolClassMap.put((byte)10, "com.lttldrgn.portochat.common.protocol.ServerKeyAccepted");
        protocolClassMap.put((byte)11, "com.lttldrgn.portochat.common.protocol.ChatMessage");
        protocolClassMap.put((byte)12, "com.lttldrgn.portochat.common.protocol.UserList");
        protocolClassMap.put((byte)13, "com.lttldrgn.portochat.common.protocol.ChannelStatus");
        protocolClassMap.put((byte)14, "com.lttldrgn.portochat.common.protocol.ChannelList");
        protocolClassMap.put((byte)15, "com.lttldrgn.portochat.common.protocol.ChannelJoinPart");
        protocolClassMap.put((byte)16, "com.lttldrgn.portochat.common.protocol.UserDoesNotExist");

        protocolHeaderMap = new HashMap<>();
        for (Map.Entry<Byte, String> entry : protocolClassMap.entrySet()) {
            protocolHeaderMap.put(entry.getValue(), entry.getKey());
        }
    }
    
    /**
     * This method processes the byte array and returns a list of DefaultData objects.
     * 
     * @param data the byte array
     * @param length the length of readable portions of the byte array
     * 
     * @return a List&ltDefaultData&gt of all the processed DefaultData objects
     */
    public List<DefaultData> processData(byte[] data, int length) {
        ArrayList<DefaultData> defaultDataList = new ArrayList<>();

        int index = 0;
        while (index < length) {
            // Byte 0 is the message type
            String protocolClassString = protocolClassMap.get(data[index]);
            if (protocolClassString != null) {
                try {
                    Class protocolClass = Class.forName(protocolClassString);
                    Constructor cons = protocolClass.getConstructor();
                    byte[] chunk;
                    chunk = Arrays.copyOfRange(data, index, data.length);
                    DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(chunk));

                    DefaultData defaultData = (DefaultData)cons.newInstance();
                    defaultData.parse(dis);
                    defaultDataList.add(defaultData);
                    int dataLength = defaultData.getLength();
                    index += dataLength;
                    if (dataLength <= 0) {
                        logger.log(Level.SEVERE, "Invalid data length: {0}", dataLength);
                        break;
                    }
                }
                catch (InstantiationException | IllegalAccessException | 
                        IllegalArgumentException | InvocationTargetException | 
                        NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                    logger.log(Level.SEVERE, "Error decoding data", ex);
                }
            } else {
                logger.log(Level.SEVERE, "Unknown protocol to decode: {0}", data[index]);
                break;
            }
        }
        return defaultDataList;
    }

    /**
     * Gets the header of the DefaultData class
     * 
     * @param clazz The DefaultData class
     * 
     * @return the byte value of the header for the DefaultData class
     */
    public Byte getHeader(Class clazz) {
        return protocolHeaderMap.get(clazz.getName());
    }
}

