package se.leap.bitmaskclient.base.fragments;

import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningKCP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseObfs4;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUsePortHopping;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseSnowflake;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.hasSnowflakePrefs;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setObfuscationPinningKCP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setUseObfs4;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setUsePortHopping;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.useSnowflake;
import static se.leap.bitmaskclient.base.utils.ViewHelper.setActionBarSubtitle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.databinding.FCensorshipCircumventionBinding;

public class CensorshipCircumventionFragment extends Fragment {
    public static int DISCOVERY_NONE = 100200000;
    public static int DISCOVERY_SNOWFLAKE = 100200001;
    public static int DISCOVERY_INVITE_PROXY = 100200002;

    public static int TUNNELING_NONE = 100300000;
    public static int TUNNELING_OBFS4 = 100300001;
    public static int TUNNELING_OBFS4_KCP = 100300002;

    private @NonNull FCensorshipCircumventionBinding binding;

    public static CensorshipCircumventionFragment newInstance() {
        CensorshipCircumventionFragment fragment = new CensorshipCircumventionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FCensorshipCircumventionBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setActionBarSubtitle(this, R.string.censorship_circumvention);
        initDiscovery();
        initTunneling();
        initPortHopping();
    }


    private void initDiscovery() {

        RadioButton noneRadioButton = new RadioButton(binding.getRoot().getContext());
        noneRadioButton.setText(getText(R.string.none));
        noneRadioButton.setId(DISCOVERY_NONE);
        noneRadioButton.setChecked(!(hasSnowflakePrefs() && getUseSnowflake()) && !ProviderObservable.getInstance().getCurrentProvider().hasIntroducer());
        binding.discoveryRadioGroup.addView(noneRadioButton);

        if (hasSnowflakePrefs()) {
            RadioButton snowflakeRadioButton = new RadioButton(binding.getRoot().getContext());
            snowflakeRadioButton.setText(getText(R.string.snowflake));
            snowflakeRadioButton.setId(DISCOVERY_SNOWFLAKE);
            snowflakeRadioButton.setChecked(hasSnowflakePrefs() && getUseSnowflake());
            binding.discoveryRadioGroup.addView(snowflakeRadioButton);
        }

        if (ProviderObservable.getInstance().getCurrentProvider().hasIntroducer()){
            RadioButton inviteProxyRadioButton = new RadioButton(binding.getRoot().getContext());
            inviteProxyRadioButton.setText(getText(R.string.invite_proxy));
            inviteProxyRadioButton.setId(DISCOVERY_INVITE_PROXY);
            inviteProxyRadioButton.setChecked(true);
            binding.discoveryRadioGroup.addView(inviteProxyRadioButton);
        }

        binding.discoveryRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == DISCOVERY_NONE) {
                useSnowflake(false);
            } else if (checkedId == DISCOVERY_SNOWFLAKE) {
                useSnowflake(true);
            } else if (checkedId == DISCOVERY_INVITE_PROXY) {
                useSnowflake(false);
            }
        });
    }


    private void initTunneling() {
        RadioButton noneRadioButton = new RadioButton(binding.getRoot().getContext());
        noneRadioButton.setText(getText(R.string.none));
        noneRadioButton.setChecked(!getUseObfs4() && !getObfuscationPinningKCP());
        noneRadioButton.setId(TUNNELING_NONE);
        binding.tunnelingRadioGroup.addView(noneRadioButton);
        if (ProviderObservable.getInstance().getCurrentProvider().supportsPluggableTransports()){
            RadioButton obfs4RadioButton = new RadioButton(binding.getRoot().getContext());
            obfs4RadioButton.setText(getText(R.string.tunnelling_obfs4));
            obfs4RadioButton.setId(TUNNELING_OBFS4);
            obfs4RadioButton.setChecked(getUseObfs4());
            binding.tunnelingRadioGroup.addView(obfs4RadioButton);

            RadioButton obfs4KcpRadioButton = new RadioButton(binding.getRoot().getContext());
            obfs4KcpRadioButton.setText(getText(R.string.tunnelling_obfs4_kcp));
            obfs4KcpRadioButton.setId(TUNNELING_OBFS4_KCP);
            obfs4KcpRadioButton.setChecked(getObfuscationPinningKCP());
            binding.tunnelingRadioGroup.addView(obfs4KcpRadioButton);
        }

        binding.tunnelingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == TUNNELING_NONE) {
                setObfuscationPinningKCP(false);
                setUseObfs4(false);
            } else if (checkedId == TUNNELING_OBFS4) {
                setObfuscationPinningKCP(false);
                setUseObfs4(true);
            } else if (checkedId == TUNNELING_OBFS4_KCP) {
                setObfuscationPinningKCP(true);
                setUseObfs4(false);
            }

        });
    }

    private void initPortHopping() {
        binding.portHoppingSwitch.findViewById(R.id.material_icon).setVisibility(View.GONE);
        binding.portHoppingSwitch.setChecked(getUsePortHopping());
        binding.portHoppingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            setUsePortHopping(isChecked);
        });
    }
}