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
            selected_provider = new Provider(new URL((provider_main_url)));
            adapter.add(selected_provider);
            adapter.saveProviders();
            autoSelectProvider(selected_provider);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void autoSelectProvider(Provider provider) {
        selected_provider = provider;
        onItemSelectedUi();
        onItemSelectedLogic();
    }

    /**
     * Asks ProviderAPI to download a new provider.json file
     *
     */
    public void setUpProvider() {
        mConfigState.setAction(SETTING_UP_PROVIDER);
        Intent provider_API_command = new Intent(this, ProviderAPI.class);
        Bundle parameters = new Bundle();
        parameters.putString(Provider.MAIN_URL, selected_provider.getMainUrl().toString());
        if (selected_provider.hasCertificatePin()){
            parameters.putString(Provider.CA_CERT_FINGERPRINT, selected_provider.certificatePin());
        }
        if (selected_provider.hasCaCert()) {
            parameters.putString(Provider.CA_CERT, selected_provider.getCaCert());
        }
        if (selected_provider.hasDefinition()) {
            parameters.putString(Provider.KEY, selected_provider.getDefinition().toString());
        }

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);

        startService(provider_API_command);
    }

    @Override
    public void retrySetUpProvider() {
        cancelSettingUpProvider();
        if (!ProviderAPI.caCertDownloaded()) {
            addAndSelectNewProvider(ProviderAPI.lastProviderMainUrl());
        } else {
            showProgressBar();
            adapter.hideAllBut(adapter.indexOf(selected_provider));


            Intent provider_API_command = new Intent(this, ProviderAPI.class);
            provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
            provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
            Bundle parameters = new Bundle();
            parameters.putString(Provider.MAIN_URL, selected_provider.getMainUrl().toString());
            provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);

            startService(provider_API_command);
        }
    }

    @Override
    public void updateProviderDetails() {
        mConfigState.setAction(SETTING_UP_PROVIDER);
        Intent provider_API_command = new Intent(this, ProviderAPI.class);

        provider_API_command.setAction(ProviderAPI.UPDATE_PROVIDER_DETAILS);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
        Bundle parameters = new Bundle();
        parameters.putString(Provider.MAIN_URL, selected_provider.getMainUrl().toString());
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);

        startService(provider_API_command);
    }

}
