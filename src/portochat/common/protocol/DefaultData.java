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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds all the default data for any message sent on the wire.
 * 
 * @author Mike
 */
public abstract class DefaultData {

    private static final Logger logger = Logger.getLogger(DefaultData.class.getName());
    protected long time = 0;
    protected int length = 0;
    
    /**
     * Public constructor for the default data class
     */
    public DefaultData() {
    }

    /**
     * Parses the data input stream
     * 
     * @param dis the data input stream
     */
    public void parse(DataInputStream dis) {

        try {
            dis.skipBytes(1); // header
            length = dis.readInt();
            time = dis.readLong();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    /**
     * Writes the header to the data output stream
     * 
     * @param dos The data output stream
     */
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

    /**
     * Writes the body data to the data output stream
     * 
     * @param dos The data output stream
     */
    public abstract int writeBody(DataOutputStream dos);

    /**
     * Converts the data into a byte array
     * 
     * @return the byte array representation of the data
     */
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

    /**
     * Populates certain data required for this object
     */
    public void populate() {
        time = new Date().getTime();
    }

    /**
     * Gets the time
     * 
     * @return the time in milliseconds since Jan 1, 1970
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the time
     * 
     * @param time the time in milliseconds since Jan 1, 1970
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * @return the length of this data in bytes
     */
    public int getLength() {
        return length;
    }

    /**
     * Sets the length of this data
     * 
     * @param length the length in bytes
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * This method is used to return a log string of the data contained
     * in this object.
     */
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
    
    /**
     * Overridden toString method
     */    
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
    
    /**
     * @return the object name
     */
    public String getObjectName() {
        return "DefaultData";
    }
}
