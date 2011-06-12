/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author Mike
 */
public class ProtocolHandler {

    private Map<Byte, String> protocolClassMap = null;
    private Map<String, Byte> protocolHeaderMap = null;
    private static ProtocolHandler instance = null;
    private static final Logger logger = Logger.getLogger(ProtocolHandler.class.getName());
    
    private ProtocolHandler() {
    }

    public static ProtocolHandler getInstance() {
        if (instance == null) {
            instance = new ProtocolHandler();
            instance.initialize();
        }

        return instance;
    }

    private void initialize() {
        protocolClassMap = new HashMap<Byte, String>();
        protocolClassMap.put((byte)0x1, "portochat.common.protocol.ServerMessage");
        protocolClassMap.put((byte)0x2, "portochat.common.protocol.Ping");
        protocolClassMap.put((byte)0x3, "portochat.common.protocol.Pong");
        protocolClassMap.put((byte)0x4, "portochat.common.protocol.UserData");
        protocolClassMap.put((byte)0x5, "portochat.common.protocol.ChatMessage");

        protocolHeaderMap = new HashMap<String, Byte>();
        protocolHeaderMap.put("portochat.common.protocol.ServerMessage", (byte)0x1);
        protocolHeaderMap.put("portochat.common.protocol.Ping", (byte)0x2);
        protocolHeaderMap.put("portochat.common.protocol.Pong", (byte)0x3);
        protocolHeaderMap.put("portochat.common.protocol.UserData", (byte)0x4);
        protocolHeaderMap.put("portochat.common.protocol.ChatMessage", (byte)0x5);
    }
    
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

    public Byte getHeader(Class clazz) {

        Byte header = null;
        header = protocolHeaderMap.get(clazz.getName());
        return header;
    }
}

