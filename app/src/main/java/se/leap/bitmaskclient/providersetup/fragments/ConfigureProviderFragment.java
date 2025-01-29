package se.leap.bitmaskclient.providersetup.fragments;

import static android.app.Activity.RESULT_CANCELED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static se.leap.bitmaskclient.R.string.app_name;
import static se.leap.bitmaskclient.R.string.description_configure_provider;
import static se.leap.bitmaskclient.R.string.description_configure_provider_circumvention;
import static se.leap.bitmaskclient.base.fragments.CensorshipCircumventionFragment.TUNNELING_AUTOMATICALLY;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.utils.BuildConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseSnowflake;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.hasSnowflakePrefs;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setUsePortHopping;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setUseTunnel;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.useSnowflake;
import static se.leap.bitmaskclient.base.utils.ViewHelper.animateContainerVisibility;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.MISSING_NETWORK_CONNECTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastLogs;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastSnowflakeLog;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastTorLog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.os.BundleCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Constants;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.FConfigureProviderBinding;
import se.leap.bitmaskclient.eip.EipSetupListener;
import se.leap.bitmaskclient.eip.EipSetupObserver;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.ProviderSetupObservable;
import se.leap.bitmaskclient.providersetup.TorLogAdapter;
import se.leap.bitmaskclient.providersetup.activities.CancelCallback;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ConfigureProviderFragment extends BaseSetupFragment implements PropertyChangeListener, CancelCallback, EipSetupListener {

    private static final String TAG = ConfigureProviderFragment.class.getSimpleName();

    FConfigureProviderBinding binding;
    private boolean isExpanded = false;
    private boolean ignoreProviderAPIUpdates = false;
    private TorLogAdapter torLogAdapter;

    private Handler handler = new Handler(Looper.getMainLooper());


    public static ConfigureProviderFragment newInstance(int position) {
        ConfigureProviderFragment fragment = new ConfigureProviderFragment();
        fragment.setArguments(initBundle(position));
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        torLogAdapter = new TorLogAdapter(getLastLogs());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FConfigureProviderBinding.inflate(inflater, container, false);
        binding.detailContainer.setVisibility(getUseSnowflake() ? VISIBLE : GONE);
        binding.tvCircumventionDescription.setText(getUseSnowflake() ? getString(description_configure_provider_circumvention, getString(app_name)) : getString(description_configure_provider, getString(app_name)));
        binding.detailHeaderContainer.setOnClickListener(v -> {
            binding.ivExpand.animate().setDuration(250).rotation(isExpanded ? -90 : 0);
            showConnectionDetails();
            animateContainerVisibility(binding.expandableDetailContainer, isExpanded);
            isExpanded = !isExpanded;
        });

        binding.ivExpand.animate().setDuration(0).rotation(270);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        binding.connectionDetailLogs.setLayoutManager(layoutManager);
        binding.connectionDetailLogs.setAdapter(torLogAdapter);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupActivityCallback.registerCancelCallback(this);
        ProviderSetupObservable.getInstance().addObserver(this);
        EipSetupObserver.addListener(this);
    }

    @Override
    public void onDestroyView() {
        setupActivityCallback.removeCancelCallback(this);
        ProviderSetupObservable.getInstance().deleteObserver(this);
        EipSetupObserver.removeListener(this);
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        ignoreProviderAPIUpdates = false;
        binding.detailContainer.setVisibility(!VpnStatus.isVPNActive() && hasSnowflakePrefs() && getUseSnowflake() ? VISIBLE : GONE);
        binding.tvCircumventionDescription.setText(getUseSnowflake() ? getString(description_configure_provider_circumvention, getString(app_name)) : getString(description_configure_provider, getString(app_name)));
        if (!isDefaultBitmask()) {
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.setup_progress_spinner, null);
            binding.progressSpinner.setAnimatedSpinnerDrawable(drawable);
        }
        binding.progressSpinner.update(ProviderSetupObservable.getProgress());
        setupActivityCallback.setNavigationButtonHidden(true);
        setupActivityCallback.setCancelButtonHidden(false);
        if (ProviderSetupObservable.isSetupRunning()) {
            handleResult(ProviderSetupObservable.getResultCode(), ProviderSetupObservable.getResultData(), true);
        } else {
            Provider provider = setupActivityCallback.getSelectedProvider();
            if (provider != null && provider.hasIntroducer()) {
                // enable automatic selection of bridges
                useSnowflake(false);
                setUseTunnel(TUNNELING_AUTOMATICALLY);
                setUsePortHopping(false);
                PreferenceHelper.useBridges(true);
            }
            ProviderSetupObservable.startSetup();
            Bundle parameters = new Bundle();
            parameters.putString(Constants.COUNTRYCODE, PreferenceHelper.getBaseCountry());
            ProviderAPICommand.execute(getContext(), SET_UP_PROVIDER, parameters, setupActivityCallback.getSelectedProvider());
        }
    }

    protected void showConnectionDetails() {

        binding.connectionDetailLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE) {
                    torLogAdapter.postponeUpdate = true;
                } else if (newState == SCROLL_STATE_IDLE && getFirstVisibleItemPosion() == 0) {
                    torLogAdapter.postponeUpdate = false;
                }
            }
        });

        binding.snowflakeState.setText(getLastSnowflakeLog());
        binding.torState.setText(getLastTorLog());
    }

    private int getFirstVisibleItemPosion() {
        return ((LinearLayoutManager) binding.connectionDetailLogs.getLayoutManager()).findFirstVisibleItemPosition();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ProviderSetupObservable.PROPERTY_CHANGE.equals(evt.getPropertyName())) {
            Activity activity = getActivity();
            if (activity == null || binding == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                if (TorStatusObservable.getStatus() != TorStatusObservable.TorStatus.OFF) {
                    if (binding.connectionDetailContainer.getVisibility() == GONE) {
                        showConnectionDetails();
                    } else {
                        setLogs(getLastTorLog(), getLastSnowflakeLog(), getLastLogs());
                    }
                }
                binding.tvProgressStatus.setText(TorStatusObservable.getStringForCurrentStatus(activity));
                binding.progressSpinner.update(ProviderSetupObservable.getProgress());
            });
        }
    }

    protected void setLogs(String torLog, String snowflakeLog, List<String> lastLogs) {
        torLogAdapter.updateData(lastLogs);
        binding.torState.setText(torLog);
        binding.snowflakeState.setText(snowflakeLog);
    }

    @Override
    public void onCanceled() {
        ignoreProviderAPIUpdates = true;
    }

    @Override
    public void handleEipEvent(Intent intent) {}

    @Override
    public void handleProviderApiEvent(Intent intent) {
        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
        Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
        if (resultData == null) {
            resultData = Bundle.EMPTY;
        }
       handleResult(resultCode, resultData, false);
    }

    private void handleResult(int resultCode, Bundle resultData, boolean resumeSetup) {
        Provider provider = BundleCompat.getParcelable(resultData, PROVIDER_KEY, Provider.class);

        if (ignoreProviderAPIUpdates ||
                provider == null ||
                (setupActivityCallback.getSelectedProvider() != null &&
                        !setupActivityCallback.getSelectedProvider().getMainUrl().equals(provider.getMainUrl()))) {
            return;
        }

        switch (resultCode) {
            case PROVIDER_OK:
                setupActivityCallback.onProviderSelected(provider);
                if (provider.getApiVersion() < 5) {
                    if (provider.allowsAnonymous()) {
                        ProviderAPICommand.execute(this.getContext(), DOWNLOAD_VPN_CERTIFICATE, provider);
                    } else {
                        // TODO: implement error message that this client only supports anonymous usage
                    }
                } else {
                    sendSuccess(resumeSetup);
                }
                break;
            case CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                setupActivityCallback.onProviderSelected(provider);
                sendSuccess(resumeSetup);
                break;
            case PROVIDER_NOK:
            case INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
            case MISSING_NETWORK_CONNECTION:
            case TOR_EXCEPTION:
            case TOR_TIMEOUT:
                String reasonToFail = resultData.getString(ERRORS);
                setupActivityCallback.onError(reasonToFail);
                break;

        }
    }

    private void sendSuccess(boolean resumeSetup) {
        handler.postDelayed(() -> {
            if (!ProviderSetupObservable.isCanceled()) {
                try {
                    setupActivityCallback.onConfigurationSuccess();
                } catch (NullPointerException npe) {
                    // callback disappeared in the meanwhile
                }
            }
        }, resumeSetup ? 0 : 750);
    }
}