/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class DefaultData {

    private static final Logger logger = Logger.getLogger(DefaultData.class.getName());
    protected long time = 0;
    protected int length = 0;
    
    public DefaultData() {
    }

    public void parse(DataInputStream dis) {

        try {
            dis.skipBytes(1); // header
            length = dis.readInt();
            time = dis.readLong();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    public int writeHeader(DataOutputStream dos, Class clazz) {

        try {
            dos.writeByte(ProtocolHandler.getInstance().getHeader(clazz));
            length += 1 + 4 + 8; //byte, int, long
            dos.writeInt(length);
            dos.writeLong(time);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write header", ex);
        }

        return dos.size();
    }

    public int writeBody(DataOutputStream dos) {
        return 0;
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        ByteArrayOutputStream bodyBaos = new ByteArrayOutputStream();
        DataOutputStream bodyDos = new DataOutputStream(bodyBaos);

        writeBody(bodyDos);
        length = bodyDos.size();
        writeHeader(dos, this.getClass());
        try {
            dos.write(bodyBaos.toByteArray());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return baos.toByteArray();
    }

    public void populate() {
        time = new Date().getTime();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getObjectName() {
        return "DefaultData";
    }

    public String toLogString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getObjectName());
        sb.append("\n");
        sb.append("Length: ");
        sb.append(length);
        sb.append("\n");
        sb.append("Time: ");
        sb.append(time);

        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(getObjectName());
        sb.append("\n");
        sb.append("Length: ");
        sb.append(length);
        sb.append("\n");
        sb.append("Time: ");
        sb.append(time);

        return sb.toString();
    }
}
