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
package se.leap.bitmaskclient.eip;

/**
 * EIPConstants for intent passing, shared preferences
 *
 * @author Parménides GV <parmegv@sdf.org>
 */
public interface EIPConstants {

    String TAG = "Constants";

    String ACTION_CHECK_CERT_VALIDITY = TAG + ".CHECK_CERT_VALIDITY";
    String ACTION_START_EIP = TAG + ".START_EIP";
    String ACTION_STOP_EIP = TAG + ".STOP_EIP";
    String ACTION_UPDATE_EIP_SERVICE = TAG + ".UPDATE_EIP_SERVICE";
    String ACTION_IS_EIP_RUNNING = TAG + ".IS_RUNNING";
    String EIP_NOTIFICATION = TAG + ".EIP_NOTIFICATION";
    String ALLOWED_ANON = "allow_anonymous";
    String ALLOWED_REGISTERED = "allow_registration";
    String VPN_CERTIFICATE = "cert";
    String PRIVATE_KEY = TAG + ".PRIVATE_KEY";
    String KEY = TAG + ".KEY";
    String RECEIVER_TAG = TAG + ".RECEIVER_TAG";
    String REQUEST_TAG = TAG + ".REQUEST_TAG";
    String START_BLOCKING_VPN_PROFILE = TAG + ".START_BLOCKING_VPN_PROFILE";
    String PROVIDER_CONFIGURED = TAG + ".PROVIDER_CONFIGURED";

}
