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
package portochat.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class can be used to check the latest version specified on the 
 * port-o-chat.googlecode.com site.  Once the version is retrieved using
 * getLatestVersion, the matchesCurrentVersion can be called to see if the 
 * current version is the same.
 * 
 */
public class VersionInfo {

    public static final String CURRENT_VERSION = "20140826";
    private static final Logger logger = Logger.getLogger(VersionInfo.class.getName());

    /**
     * Retrieves the latest version of the software from the source site.  An 
     * IOException is thrown if the server can not be contacted or an error 
     * occurs during retrieval.
     * @return String representing the latest version of the software
     * @throws IOException
     */
    public static String getLatestVersion() throws IOException {

        URL portochatVersion = new URL("http://port-o-chat.googlecode.com/svn/wiki/latest_version.txt");
        URLConnection urlConnection = portochatVersion.openConnection();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(urlConnection.getInputStream()));

        String currentVersion = reader.readLine();

        return currentVersion;
    }
    
    /**
     * Returns whether or not the running software is the same version as the
     * latest available on the googlecode site.  An IOException is thrown if
     * the server can not be contacted or an error occurs during retrieval.
     * @return True if the running software is up to date
     * @throws IOException 
     */
    public static boolean isSoftwareCurrent() throws IOException{
        return CURRENT_VERSION.equals(getLatestVersion());
    }
    
    public static void main(String args[]) {
        try {
            System.out.println(getLatestVersion());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error retrieving latest software version", ex);
        }
    }
    
}