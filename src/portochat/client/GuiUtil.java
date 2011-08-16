/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package portochat.client;

import java.awt.Point;
import java.util.prefs.Preferences;

/**
 *
 * @author Brandon
 */
public class GuiUtil {
    private static final int X_DEFAULT = 0, Y_DEFAULT = 0;

    private static final String POSITION = "Position";
    private static final String USERNAME = "username";
    private static final String SERVER = "server";

    /**
     * Retrieves the last window position saved to preferences
     * @param clazz Class calling this method.  Used to generate pref location.
     * @param nodeName Preference node name.
     * @return Point representing the last saved location of the window
     */
    public static Point getLastWindowPosition(Class<?> clazz, String nodeName) {
        Point lastPos = new Point();
        Preferences prefs = Preferences.userNodeForPackage(clazz).node(nodeName);
        String position = prefs.get(POSITION, X_DEFAULT + "," + Y_DEFAULT);
        try {
            int i = position.indexOf(',');
            lastPos.x = Integer.parseInt(position.substring(0, i));
            lastPos.y = Integer.parseInt(position.substring(i + 1));
        } catch(Exception e) {
            // Value was corrupt, just use defaults
            lastPos.x = X_DEFAULT;
            lastPos.y = Y_DEFAULT;
        }

        return lastPos;
    }

    /**
     * Saves a window position to preferences
     * @param clazz Class calling this method.  Used to generate pref location.
     * @param nodeName Preference node name.
     * @param location Location of window.
     */
    public static void saveWindowPosition(Class<?> clazz, String nodeName, Point location) {
        Preferences prefs = Preferences.userNodeForPackage(clazz).node(nodeName);
        prefs.put(POSITION, location.x + "," + location.y);
    }
    
    /**
     * Store the user name for later retrieval
     * @param clazz Class calling this method
     * @param userName Username to save
     */
    public static void saveUserName(Class<?> clazz, String userName) {
        Preferences prefs = Preferences.userNodeForPackage(clazz);
        prefs.put(USERNAME, userName);
    }
    
    /**
     * Get the user name that is saved in the preferences
     * @param clazz Class calling this method
     * @return Saved user name or "user" if none is saved
     */
    public static String getUserName(Class<?> clazz) {
        Preferences prefs = Preferences.userNodeForPackage(clazz);
        return prefs.get(USERNAME, "user");
    }
    
    /**
     * Save server name to preferences
     * @param clazz Class calling this method
     * @param serverName Server name to write to preferences
     */
    public static void saveServerName(Class<?> clazz, String serverName) {
        Preferences prefs = Preferences.userNodeForPackage(clazz);
        prefs.put(SERVER, serverName);
    }
    
    /**
     * Get the last server name used
     * @param clazz Class calling this method
     * @return Last server name saved
     */
    public static String getServerName(Class<?> clazz) {
        Preferences prefs = Preferences.userNodeForPackage(clazz);
        return prefs.get(SERVER, "localhost");
    }
}
