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
package com.lttldrgn.portochat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.crypto.SecretKey;
import com.lttldrgn.portochat.common.network.handler.BufferHandler;
import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author Mike
 */
public class User {
    private String id;
    private String name = null;
    private String host = null;
    private long lastSeen;
    private SecretKey secretKey = null;
    private PublicKey clientPublicKey = null;
    private List<BufferHandler> handlers = null;
    
    /**
     * User constructor
     */
    public User() {
        id = UUID.randomUUID().toString();
        handlers = new CopyOnWriteArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the user's host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the user's host
     * 
     * @param host 
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return Returns the user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name
     * 
     * @param name 
     */
    public void setName(String name) {
        this.name = name;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public PublicKey getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(PublicKey clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void clearHandlers() {
        handlers.clear();
    }

    public List<BufferHandler> getHandlers() {
        return handlers;
    }
      
    public void addHandler(BufferHandler handler) {
        handlers.add(handler);
    }
    
    public void removeHandler(BufferHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.id);
        hash = 29 * hash + Objects.hashCode(this.name);
        hash = 29 * hash + Objects.hashCode(this.host);
        return hash;
    }

    @Override
    public String toString() {
        return name + " (" + host + ")";
    }
}
