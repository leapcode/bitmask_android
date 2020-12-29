/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributors
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
import androidx.annotation.NonNull;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.activities.ProviderListBaseActivity;

import static se.leap.bitmaskclient.base.models.Constants.DANGER_ON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SETTING_UP_PROVIDER;

/**
 * Activity that builds and shows the list of known available providers.
 * <p/>
 * It also allows the user to enter custom providers with a button.
 *
 * @author parmegv
 * @author cyberta
 */
public class ProviderListActivity extends ProviderListBaseActivity {

    @Override
    protected void onItemSelectedLogic() {
        boolean danger_on = preferences.getBoolean(DANGER_ON, true);
        setUpProvider(danger_on);
    }

    @Override
    public void cancelSettingUpProvider() {
        super.cancelSettingUpProvider();
        preferences.edit().remove(DANGER_ON).apply();
    }

    /**
     * Asks ProviderAPI to download a new provider.json file
     *
     * @param danger_on tells if HTTPS client should bypass certificate errors
     */
    public void setUpProvider(boolean danger_on) {
        providerConfigState = SETTING_UP_PROVIDER;

        Bundle parameters = new Bundle();
        parameters.putBoolean(DANGER_ON, danger_on);

        ProviderAPICommand.execute(this, SET_UP_PROVIDER, parameters, provider);
    }

    /**
     * Retrys setup of last used provider, allows bypassing ca certificate validation.
     */
    @Override
    public void retrySetUpProvider(@NonNull Provider provider) {
        providerConfigState = SETTING_UP_PROVIDER;
        ProviderAPICommand.execute(this, SET_UP_PROVIDER, provider);
    }

}
