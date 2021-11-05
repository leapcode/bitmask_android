/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.providersetup;

import android.os.Bundle;

import se.leap.bitmaskclient.base.models.Provider;

/**
 * Created by cyberta on 17.08.18.
 */

public interface ProviderSetupInterface {
    enum ProviderConfigState {
        PROVIDER_NOT_SET,
        SETTING_UP_PROVIDER,
        SHOWING_PROVIDER_DETAILS,
        PENDING_SHOW_PROVIDER_DETAILS,
        PENDING_SHOW_FAILED_DIALOG,
        SHOW_FAILED_DIALOG,
    }

    void handleProviderSetUp(Provider provider);
    void handleError(Bundle resultData);
    void handleCorrectlyDownloadedCertificate(Provider provider);
    void handleIncorrectlyDownloadedCertificate();
    Provider getProvider();
    ProviderConfigState getConfigState();
}
