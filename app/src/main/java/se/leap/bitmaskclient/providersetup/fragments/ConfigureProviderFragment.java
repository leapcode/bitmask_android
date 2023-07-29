package se.leap.bitmaskclient.providersetup.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static se.leap.bitmaskclient.base.utils.ViewHelper.animateContainerVisibility;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getBootstrapProgress;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastLogs;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastSnowflakeLog;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastTorLog;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.FConfigureProviderBinding;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.TorLogAdapter;
import se.leap.bitmaskclient.providersetup.activities.SetupInterface;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ConfigureProviderFragment extends Fragment implements Observer {

    public static ConfigureProviderFragment newInstance(int position) {
        return new ConfigureProviderFragment(position);
    }

    FConfigureProviderBinding binding;
    private SetupInterface setupInterface;
    private boolean isExpanded = false;
    private final int position;
    private ViewPager2.OnPageChangeCallback viewPagerCallback;
    private TorLogAdapter torLogAdapter;


    public ConfigureProviderFragment(int position) {
        this.position = position;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FConfigureProviderBinding.inflate(inflater, container, false);
        binding.detailContainer.setVisibility(PreferenceHelper.getUseSnowflake(getContext()) ? VISIBLE : GONE);
        binding.detailHeaderContainer.setOnClickListener(v -> {
            binding.ivExpand.animate().setDuration(250).rotation(isExpanded ? 0 : 270);
            showConnectionDetails();
            animateContainerVisibility(binding.expandableDetailContainer, isExpanded);
            isExpanded = !isExpanded;
        });

        binding.ivExpand.animate().setDuration(0).rotation(270);
        return binding.getRoot();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setupInterface = (SetupInterface) getActivity();
        viewPagerCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == ConfigureProviderFragment.this.position) {
                    binding.detailContainer.setVisibility(PreferenceHelper.getUseSnowflake(getContext()) ? VISIBLE : GONE);
                    setupInterface.setNavigationButtonHidden(true);
                    ProviderAPICommand.execute(context, SET_UP_PROVIDER, setupInterface.getSelectedProvider());
                }
            }
        };
        setupInterface.registerOnPageChangeCallback(viewPagerCallback);
        TorStatusObservable.getInstance().addObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        TorStatusObservable.getInstance().deleteObserver(this);
        setupInterface.removeOnPageChangeCallback(viewPagerCallback);
        setupInterface = null;
    }

    protected void showConnectionDetails() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        binding.connectionDetailLogs.setLayoutManager(layoutManager);
        torLogAdapter = new TorLogAdapter(getLastLogs());
        binding.connectionDetailLogs.setAdapter(torLogAdapter);

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
                binding.progressSpinner.update(getBootstrapProgress());
            });
        }
    }

    protected void setLogs(String torLog, String snowflakeLog, List<String> lastLogs) {
        torLogAdapter.updateData(lastLogs);
        binding.torState.setText(torLog);
        binding.snowflakeState.setText(snowflakeLog);
    }
}