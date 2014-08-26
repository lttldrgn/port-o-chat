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
package portochat.common.protocol;

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
        protocolClassMap = new HashMap<Byte, String>();
        protocolClassMap.put((byte)0x1, "portochat.common.protocol.ServerMessage");
        protocolClassMap.put((byte)0x2, "portochat.common.protocol.Initialization");
        protocolClassMap.put((byte)0x3, "portochat.common.protocol.Ping");
        protocolClassMap.put((byte)0x4, "portochat.common.protocol.Pong");
        protocolClassMap.put((byte)0x5, "portochat.common.protocol.UserConnection");
        protocolClassMap.put((byte)0x6, "portochat.common.protocol.UserData");
        protocolClassMap.put((byte)0x7, "portochat.common.protocol.ChatMessage");
        protocolClassMap.put((byte)0x8, "portochat.common.protocol.UserList");
        protocolClassMap.put((byte)0x9, "portochat.common.protocol.ChannelStatus");
        protocolClassMap.put((byte)0x10, "portochat.common.protocol.ChannelList");
        protocolClassMap.put((byte)0x11, "portochat.common.protocol.ChannelJoinPart");

        protocolHeaderMap = new HashMap<String, Byte>();
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
     * @return a List<DefaultData> of all the processed DefaultData objects
     */
    public List<DefaultData> processData(byte[] data, int length) {
        ArrayList<DefaultData> defaultDataList = new ArrayList<DefaultData>();

        int index = 0;
        while (index < length) {
            // Bit 0 is the message type
            String protocolClassString = protocolClassMap.get(data[index]);
            if (protocolClassString != null) {
                try {
                    Class protocolClass = Class.forName(protocolClassString);
                    Constructor cons = protocolClass.getConstructor();
                    byte[] chunk = null;
                    chunk = Arrays.copyOfRange(data, index, data.length);
                    DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(chunk));

                    DefaultData defaultData = (DefaultData)cons.newInstance();
                    defaultData.parse(dis);
                    defaultDataList.add(defaultData);
                    index += defaultData.getLength();
                }
                catch (InstantiationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    logger.log(Level.SEVERE, null, ex);
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

        Byte header = null;
        header = protocolHeaderMap.get(clazz.getName());
        return header;
    }
}
