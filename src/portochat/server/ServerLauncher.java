/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.server;

import portochat.common.Settings;

/**
 *
 * @author Mike
 */
public class ServerLauncher {
    public static void main (String args[]) {
        Server server = new Server();
        server.listen(Settings.DEFAULT_SERVER_PORT);
    }
}
