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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;

import java.net.MalformedURLException;
import java.net.URL;

import se.leap.bitmaskclient.ProviderListContent.ProviderItem;

/**
 * Activity that builds and shows the list of known available providers.
 * <p/>
 * It also allows the user to enter custom providers with a button.
 *
 * @author parmegv
 * @author cyberta
 */
public class ConfigurationWizard extends BaseConfigurationWizard {

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
     * Open the new provider dialog with data
     */
    public void addAndSelectNewProvider(String main_url, boolean danger_on) {
        FragmentTransaction fragment_transaction = fragmentManager.removePreviousFragment(NewProviderDialog.TAG);

        DialogFragment newFragment = new NewProviderDialog();
        Bundle data = new Bundle();
        data.putString(Provider.MAIN_URL, main_url);
        data.putBoolean(ProviderItem.DANGER_ON, danger_on);
        newFragment.setArguments(data);
        newFragment.show(fragment_transaction, NewProviderDialog.TAG);
    }

    public void showAndSelectProvider(String provider_main_url, boolean danger_on) {
        try {
            provider = new Provider(new URL((provider_main_url)));
            adapter.add(provider);
            adapter.saveProviders();
            autoSelectProvider(provider, danger_on);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void autoSelectProvider(Provider provider, boolean danger_on) {
        preferences.edit().putBoolean(ProviderItem.DANGER_ON, danger_on).apply();
        this.provider = provider;
        onItemSelectedLogic();
        showProgressBar();
    }

    /**
     * Asks ProviderAPI to download a new provider.json file
     *
     * @param danger_on tells if HTTPS client should bypass certificate errors
     */
    public void setUpProvider(boolean danger_on) {
        mConfigState.setAction(SETTING_UP_PROVIDER);
        Intent providerAPICommand = new Intent(this, ProviderAPI.class);
        Bundle parameters = new Bundle();
        parameters.putString(Provider.MAIN_URL, provider.getMainUrl().toString());
        parameters.putBoolean(ProviderItem.DANGER_ON, danger_on);
        if (provider.hasCertificatePin()){
            parameters.putString(Provider.CA_CERT_FINGERPRINT, provider.certificatePin());
        }
        if (provider.hasCaCert()) {
            parameters.putString(Provider.CA_CERT, provider.getCaCert());
        }
        if (provider.hasDefinition()) {
            parameters.putString(Provider.KEY, provider.getDefinition().toString());
        }

        providerAPICommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerAPICommand.putExtra(ProviderAPI.PARAMETERS, parameters);

        startService(providerAPICommand);
    }

    /**
     * Retrys setup of last used provider, allows bypassing ca certificate validation.
     */
    @Override
    public void retrySetUpProvider() {
        cancelSettingUpProvider();
        if (!ProviderAPI.caCertDownloaded()) {
            addAndSelectNewProvider(ProviderAPI.lastProviderMainUrl(), ProviderAPI.lastDangerOn());
        } else {
            showProgressBar();

            Intent providerAPICommand = new Intent(this, ProviderAPI.class);

            providerAPICommand.setAction(ProviderAPI.SET_UP_PROVIDER);
            Bundle parameters = new Bundle();
            parameters.putString(Provider.MAIN_URL, provider.getMainUrl().toString());
            providerAPICommand.putExtra(ProviderAPI.PARAMETERS, parameters);

            startService(providerAPICommand);
        }
    }

}
