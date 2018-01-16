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

import android.content.Intent;
import android.os.Bundle;

import java.net.MalformedURLException;
import java.net.URL;

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
        setUpProvider();
    }

    public void showAndSelectProvider(String provider_main_url) {
        try {
            selectedProvider = new Provider(new URL((provider_main_url)));
            adapter.add(selectedProvider);
            adapter.saveProviders();
            autoSelectProvider(selectedProvider);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void autoSelectProvider(Provider provider) {
        selectedProvider = provider;
        onItemSelectedUi();
        onItemSelectedLogic();
    }

    /**
     * Asks ProviderAPI to download a new provider.json file
     *
     */
    public void setUpProvider() {
        mConfigState.setAction(SETTING_UP_PROVIDER);
        Intent providerApiCommand = new Intent(this, ProviderAPI.class);
        Bundle parameters = new Bundle();
        parameters.putString(Provider.MAIN_URL, selectedProvider.getMainUrl().toString());
        if (selectedProvider.hasCertificatePin()){
            parameters.putString(Provider.CA_CERT_FINGERPRINT, selectedProvider.certificatePin());
        }
        if (selectedProvider.hasCaCert()) {
            parameters.putString(Provider.CA_CERT, selectedProvider.getCaCert());
        }
        if (selectedProvider.hasDefinition()) {
            parameters.putString(Provider.KEY, selectedProvider.getDefinition().toString());
        }

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.PARAMETERS, parameters);

        startService(providerApiCommand);
    }

    @Override
    public void retrySetUpProvider() {
        cancelSettingUpProvider();
        if (!ProviderAPI.caCertDownloaded()) {
            addAndSelectNewProvider(ProviderAPI.lastProviderMainUrl());
        } else {
            showProgressBar();
            adapter.hideAllBut(adapter.indexOf(selectedProvider));


            Intent providerApiCommand = new Intent(this, ProviderAPI.class);
            providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
            providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, providerAPIResultReceiver);
            Bundle parameters = new Bundle();
            parameters.putString(Provider.MAIN_URL, selectedProvider.getMainUrl().toString());
            providerApiCommand.putExtra(ProviderAPI.PARAMETERS, parameters);

            startService(providerApiCommand);
        }
    }

}
