/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient;

public class User {
    private static String user_name = "The user";
    private static User user;
    
    public static User getInstance() {
	if(user == null) {
	    user = new User();
	}
	return user;
    }

    public static void setUserName(String user_name) {
	User.user_name = user_name;
    }

    private User() { }
    
    public static String userName() { return user_name; }

    public static boolean loggedIn() {
        return LeapSRPSession.loggedIn();
    }
}
