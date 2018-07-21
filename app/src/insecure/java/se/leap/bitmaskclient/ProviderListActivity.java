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
package se.leap.bitmaskclient;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;

import java.net.MalformedURLException;
import java.net.URL;

import se.leap.bitmaskclient.ProviderListContent.ProviderItem;

import static se.leap.bitmaskclient.ProviderAPI.SET_UP_PROVIDER;

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
        boolean danger_on = preferences.getBoolean(ProviderItem.DANGER_ON, true);
        setUpProvider(danger_on);
    }

    @Override
    public void cancelSettingUpProvider() {
        super.cancelSettingUpProvider();
        preferences.edit().remove(ProviderItem.DANGER_ON).apply();
    }

    /**
     * Asks ProviderAPI to download a new provider.json file
     *
     * @param danger_on tells if HTTPS client should bypass certificate errors
     */
    public void setUpProvider(boolean danger_on) {
        configState.setAction(SETTING_UP_PROVIDER);

        Bundle parameters = new Bundle();
        parameters.putBoolean(ProviderItem.DANGER_ON, danger_on);

        ProviderAPICommand.execute(this, SET_UP_PROVIDER, parameters, provider);
    }

    /**
     * Retrys setup of last used provider, allows bypassing ca certificate validation.
     */
    @Override
    public void retrySetUpProvider(@NonNull Provider provider) {
        configState.setAction(SETTING_UP_PROVIDER);
        ProviderAPICommand.execute(this, SET_UP_PROVIDER, provider);
    }

}
