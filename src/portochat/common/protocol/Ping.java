/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public class Ping extends DefaultData {

    private static final Logger logger = Logger.getLogger(Ping.class.getName());
    private long timestamp = 0;
    
    public Ping() {
    }
    
    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);

        try {
            timestamp = dis.readLong();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }
    
    @Override
    public int writeBody(DataOutputStream dos) {
        
        try {
            dos.writeLong(timestamp);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        
        return dos.size();
    }
    
    @Override
    public void populate() {
        super.populate();
        timestamp = System.currentTimeMillis();
    }
    
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("Ping: ");
        sb.append(timestamp);

        return sb.toString();
    }

    @Override
    public String getObjectName() {
        return "Ping";
    }
}
