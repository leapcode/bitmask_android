package se.leap.bitmaskclient.providersetup.fragments;

import static android.app.Activity.RESULT_CANCELED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
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
import static se.leap.bitmaskclient.tor.TorStatusObservable.getBootstrapProgress;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastLogs;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastSnowflakeLog;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastTorLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.FConfigureProviderBinding;
import se.leap.bitmaskclient.eip.EipSetupListener;
import se.leap.bitmaskclient.eip.EipSetupObserver;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.TorLogAdapter;
import se.leap.bitmaskclient.providersetup.activities.CancelCallback;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ConfigureProviderFragment extends BaseSetupFragment implements Observer, CancelCallback, EipSetupListener {

    private static final String TAG = ConfigureProviderFragment.class.getSimpleName();

    public static ConfigureProviderFragment newInstance(int position) {
        return new ConfigureProviderFragment(position);
    }

    FConfigureProviderBinding binding;
    private boolean isExpanded = false;
    private boolean ignoreProviderAPIUpdates = false;
    private TorLogAdapter torLogAdapter;


    private ConfigureProviderFragment(int position) {
        super(position);
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
        binding.detailContainer.setVisibility(PreferenceHelper.getUseSnowflake() ? VISIBLE : GONE);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        ignoreProviderAPIUpdates = false;
        binding.detailContainer.setVisibility(PreferenceHelper.getUseSnowflake() ? VISIBLE : GONE);
        setupActivityCallback.setNavigationButtonHidden(true);
        setupActivityCallback.setCancelButtonHidden(false);
        ProviderAPICommand.execute(getContext(), SET_UP_PROVIDER, setupActivityCallback.getSelectedProvider());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setupActivityCallback.registerCancelCallback(this);
        TorStatusObservable.getInstance().addObserver(this);
        EipSetupObserver.addListener(this);
    }

    @Override
    public void onDetach() {
        setupActivityCallback.removeCancelCallback(this);
        TorStatusObservable.getInstance().deleteObserver(this);
        EipSetupObserver.removeListener(this);
        super.onDetach();
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
    public void update(Observable o, Object arg) {
        if (o instanceof TorStatusObservable) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                if (TorStatusObservable.getStatus() != TorStatusObservable.TorStatus.OFF) {
                    if (binding.connectionDetailContainer.getVisibility() == GONE) {
                        showConnectionDetails();
                    } else {
                        setLogs(getLastTorLog(), getLastSnowflakeLog(), getLastLogs());
                    }
                }
                binding.tvProgressStatus.setText(TorStatusObservable.getStringForCurrentStatus(activity));
                binding.progressSpinner.update(getBootstrapProgress());
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
        Provider provider = resultData.getParcelable(PROVIDER_KEY);
        if (ignoreProviderAPIUpdates ||
                provider == null ||
                !setupActivityCallback.getSelectedProvider().getDomain().equals(provider.getDomain())) {
            return;
        }

        switch (resultCode) {
            case PROVIDER_OK:
                if (provider.allowsAnonymous()) {
                    ProviderAPICommand.execute(this.getContext(), DOWNLOAD_VPN_CERTIFICATE, provider);
                } else {
                    // TODO: implement error message that this client only supports anonymous usage
                }
                break;
            case CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                setupActivityCallback.onProviderSelected(provider);
                setupActivityCallback.onConfigurationSuccess();
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

}