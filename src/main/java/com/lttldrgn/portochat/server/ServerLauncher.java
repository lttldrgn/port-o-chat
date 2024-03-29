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
package com.lttldrgn.portochat.server;

import com.lttldrgn.portochat.common.Settings;

/**
 * This class is used to launch the server from the CLI
 * @author Mike
 */
public class ServerLauncher {
    
    public static void main (String args[]) {
        int port = Settings.DEFAULT_SERVER_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.out.println("Error in arguments.  Using default port " +
                        port);
            }
        }
        Server server = new Server();
        server.bind(port);
    }
}
