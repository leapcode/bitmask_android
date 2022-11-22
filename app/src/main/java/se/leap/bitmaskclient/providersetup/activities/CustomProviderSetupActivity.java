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
package se.leap.bitmaskclient.providersetup.activities;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;

import static se.leap.bitmaskclient.BuildConfig.customProviderApiIp;
import static se.leap.bitmaskclient.BuildConfig.customProviderIp;
import static se.leap.bitmaskclient.BuildConfig.customProviderMotdUrl;
import static se.leap.bitmaskclient.BuildConfig.customProviderUrl;
import static se.leap.bitmaskclient.BuildConfig.geoipUrl;
import static se.leap.bitmaskclient.base.models.Constants.EXT_JSON;
import static se.leap.bitmaskclient.base.models.Constants.EXT_PEM;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.preferAnonymousUsage;
import static se.leap.bitmaskclient.base.utils.InputStreamHelper.loadInputStreamAsString;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SETTING_UP_PROVIDER;

/**
 * Created by cyberta on 17.08.18.
 */

public class CustomProviderSetupActivity extends ProviderSetupBaseActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpInitialUI();
        restoreState(savedInstanceState);
        setDefaultProvider();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getConfigState() == ProviderConfigState.PROVIDER_NOT_SET) {
            showProgressBar();
            setupProvider();
        }
    }

    private void setDefaultProvider() {
        try {
            AssetManager assetsManager = getAssets();
            Provider customProvider = new Provider(customProviderUrl, geoipUrl, customProviderMotdUrl, customProviderIp, customProviderApiIp);
            String domain = ConfigHelper.getDomainFromMainURL(customProviderUrl);
            String certificate = loadInputStreamAsString(assetsManager.open(domain + EXT_PEM));
            String providerDefinition = loadInputStreamAsString(assetsManager.open(domain + EXT_JSON));
            customProvider.setCaCert(certificate);
            customProvider.define(new JSONObject(providerDefinition));
            setProvider(customProvider);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            setProvider(new Provider(customProviderUrl, geoipUrl, customProviderMotdUrl, customProviderIp, customProviderApiIp));
        }
    }

    private void setUpInitialUI() {
        setContentView(R.layout.a_custom_provider_setup);
        setProviderHeaderText(R.string.setup_provider);
        hideProgressBar();
    }

    private void setupProvider() {
        setProviderConfigState(SETTING_UP_PROVIDER);
        ProviderAPICommand.execute(this, SET_UP_PROVIDER, getProvider());
    }

    // ------- ProviderSetupInterface ---v
    @Override
    public void handleProviderSetUp(Provider provider) {
        setProvider(provider);
        if (provider.allowsAnonymous()) {
            downloadVpnCertificate();
        } else {
            showProviderDetails();
        }
    }

    @Override
    public void handleCorrectlyDownloadedCertificate(Provider provider) {
        if (preferAnonymousUsage()) {
            finishWithSetupWithProvider(provider);
        } else {
            this.provider = provider;
            showProviderDetails();
        }
    }

    // ------- DownloadFailedDialogInterface ---v
    @Override
    public void retrySetUpProvider(@NonNull Provider provider) {
        setupProvider();
        showProgressBar();
    }

    @Override
    public void cancelSettingUpProvider() {
        super.cancelSettingUpProvider();
        finish();
    }

    @Override
    public void addAndSelectNewProvider(String url) {
        // ignore
    }

    private void finishWithSetupWithProvider(Provider provider) {
        Intent intent = new Intent();
        intent.putExtra(Provider.KEY, provider);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP) {
            setResult(resultCode, data);
            finish();
        }
    }
}
