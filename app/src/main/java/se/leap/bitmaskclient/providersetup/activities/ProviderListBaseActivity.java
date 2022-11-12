/**
 * Copyright (c) 2017 LEAP Encryption Access Project and contributors
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package se.leap.bitmaskclient.providersetup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.pedrogomez.renderers.Renderer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnItemClick;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.AddProviderActivity;
import se.leap.bitmaskclient.providersetup.ProviderListActivity;
import se.leap.bitmaskclient.providersetup.ProviderListAdapter;
import se.leap.bitmaskclient.providersetup.ProviderRenderer;
import se.leap.bitmaskclient.providersetup.ProviderRendererBuilder;

import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_ADD_PROVIDER;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SETTING_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SHOW_FAILED_DIALOG;

/**
 * abstract base Activity that builds and shows the list of known available providers.
 * The implementation of ProviderListBaseActivity differ in that they may or may not allow to bypass
 * secure download mechanisms including certificate validation.
 * <p/>
 * It also allows the user to enter custom providers with a button.
 *
 * @author parmegv
 * @author cyberta
 */

public abstract class ProviderListBaseActivity extends ProviderSetupBaseActivity {

    @BindView(R.id.provider_list)
    protected ListView providerListView;
    @Inject
    protected ProviderListAdapter adapter;

    final public static String TAG = ProviderListActivity.class.getSimpleName();
    final protected static String EXTRAS_KEY_INVALID_URL = "INVALID_URL";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpInitialUI();
        initProviderList();
        restoreState(savedInstanceState);
    }

    public abstract void retrySetUpProvider(@NonNull Provider provider);

    protected abstract void onItemSelectedLogic();

    private void initProviderList() {
        List<Renderer<Provider>> prototypes = new ArrayList<>();
        prototypes.add(new ProviderRenderer(this));
        ProviderRendererBuilder providerRendererBuilder = new ProviderRendererBuilder(prototypes);
        adapter = new ProviderListAdapter(getLayoutInflater(), providerRendererBuilder, getProviderManager());
        providerListView.setAdapter(adapter);
    }

    private void setUpInitialUI() {
        setContentView(R.layout.a_provider_list);
        setProviderHeaderText(R.string.setup_provider);
        hideProgressBar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP) {
            if (resultCode == RESULT_OK) {
                setResult(resultCode, data);
                finish();
            }
        } else if (requestCode == REQUEST_CODE_ADD_PROVIDER) {
            if (resultCode == RESULT_OK) {
                testNewURL = true;
                String newUrl = data.getStringExtra(AddProviderActivity.EXTRAS_KEY_NEW_URL);
                this.provider.setMainUrl(newUrl);
                showAndSelectProvider(newUrl);
            } else {
                cancelSettingUpProvider();
            }
        }
    }

    public void showAndSelectProvider(String newURL) {
        provider = new Provider(newURL, null, null);
        autoSelectProvider();
    }

    private void autoSelectProvider() {
        onItemSelectedLogic();
        showProgressBar();
    }

    // ------- ProviderSetupInterface ---v
    @Override
    public void handleProviderSetUp(Provider handledProvider) {
        this.provider = handledProvider;
        adapter.add(provider);
        adapter.saveProviders();
        if (provider.allowsAnonymous()) {
            //FIXME: providerApiBroadcastReceiver.getConfigState().putExtra(SERVICES_RETRIEVED, true); DEAD CODE???
            downloadVpnCertificate();
        } else {
            showProviderDetails();
        }
    }

    @Override
    public void handleCorrectlyDownloadedCertificate(Provider handledProvider) {
        this.provider = handledProvider;
        showProviderDetails();
    }

    @OnItemClick(R.id.provider_list)
    void onItemSelected(int position) {
        if (SETTING_UP_PROVIDER == getConfigState() ||
                SHOW_FAILED_DIALOG == getConfigState()) {
            return;
        }

        //TODO Code 2 pane view
        provider = adapter.getItem(position);
        if (provider != null && !provider.isDefault()) {
            //TODO Code 2 pane view
            providerConfigState = SETTING_UP_PROVIDER;
            showProgressBar();
            onItemSelectedLogic();
        } else {
            addAndSelectNewProvider();
        }
    }

    @Override
    public void onBackPressed() {
        if (SETTING_UP_PROVIDER == providerConfigState ||
                SHOW_FAILED_DIALOG == providerConfigState) {
            cancelSettingUpProvider();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Open the new provider dialog
     */
    public void addAndSelectNewProvider() {
        Intent intent = new Intent(this, AddProviderActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_PROVIDER);
    }

    /**
     * Open the new provider dialog
     */
    @Override
    public void addAndSelectNewProvider(String url) {
        testNewURL = false;
        Intent intent = new Intent(this, AddProviderActivity.class);
        intent.putExtra(EXTRAS_KEY_INVALID_URL, url);
        startActivityForResult(intent, REQUEST_CODE_ADD_PROVIDER);
    }
    
}
