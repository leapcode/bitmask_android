package se.leap.bitmaskclient.base.fragments;

import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseObfs4;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseObfs4Kcp;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUsePortHopping;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseSnowflake;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.hasSnowflakePrefs;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.resetSnowflakeSettings;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setUsePortHopping;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setUseTunnel;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.useBridges;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.useSnowflake;
import static se.leap.bitmaskclient.base.utils.ViewHelper.setActionBarSubtitle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.databinding.FCensorshipCircumventionBinding;
import se.leap.bitmaskclient.eip.EipCommand;

public class CensorshipCircumventionFragment extends Fragment {
    public static int DISCOVERY_AUTOMATICALLY = 100200000;
    public static int DISCOVERY_SNOWFLAKE = 100200001;
    public static int DISCOVERY_INVITE_PROXY = 100200002;

    public static int TUNNELING_AUTOMATICALLY = 100300000;
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
        boolean hasIntroducer = ProviderObservable.getInstance().getCurrentProvider().hasIntroducer();
        RadioButton automaticallyRadioButton = new RadioButton(binding.getRoot().getContext());
        automaticallyRadioButton.setText(getText(R.string.automatically_select));
        automaticallyRadioButton.setId(DISCOVERY_AUTOMATICALLY);
        automaticallyRadioButton.setChecked(!hasSnowflakePrefs() && !hasIntroducer);
        binding.discoveryRadioGroup.addView(automaticallyRadioButton);

        RadioButton snowflakeRadioButton = new RadioButton(binding.getRoot().getContext());
        snowflakeRadioButton.setText(getText(R.string.snowflake));
        snowflakeRadioButton.setId(DISCOVERY_SNOWFLAKE);
        snowflakeRadioButton.setChecked(!hasIntroducer && hasSnowflakePrefs() && getUseSnowflake());
        binding.discoveryRadioGroup.addView(snowflakeRadioButton);

        if (hasIntroducer) {
            RadioButton inviteProxyRadioButton = new RadioButton(binding.getRoot().getContext());
            inviteProxyRadioButton.setText(getText(R.string.invite_proxy));
            inviteProxyRadioButton.setId(DISCOVERY_INVITE_PROXY);
            inviteProxyRadioButton.setChecked(true);
            binding.discoveryRadioGroup.addView(inviteProxyRadioButton);
        }

        binding.discoveryRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            useBridges(true);
            if (checkedId == DISCOVERY_AUTOMATICALLY) {
                resetSnowflakeSettings();
            } else if (checkedId == DISCOVERY_SNOWFLAKE) {
                useSnowflake(true);
            } else if (checkedId == DISCOVERY_INVITE_PROXY) {
                useSnowflake(false);
            }
        });
    }

    private void tryReconnectVpn() {
        if (VpnStatus.isVPNActive()) {
            EipCommand.startVPN(getContext(), false);
            Toast.makeText(getContext(), R.string.reconnecting, Toast.LENGTH_LONG).show();
        }
    }


    private void initTunneling() {
        RadioButton noneRadioButton = new RadioButton(binding.getRoot().getContext());
        noneRadioButton.setText(getText(R.string.automatically_select));
        noneRadioButton.setChecked(!getUseObfs4() && !getUseObfs4Kcp());
        noneRadioButton.setId(TUNNELING_AUTOMATICALLY);
        binding.tunnelingRadioGroup.addView(noneRadioButton);

        if (ProviderObservable.getInstance().getCurrentProvider().supportsObfs4()) {
            RadioButton obfs4RadioButton = new RadioButton(binding.getRoot().getContext());
            obfs4RadioButton.setText(getText(R.string.tunnelling_obfs4));
            obfs4RadioButton.setId(TUNNELING_OBFS4);
            obfs4RadioButton.setChecked(getUseObfs4());
            binding.tunnelingRadioGroup.addView(obfs4RadioButton);
        }

        if (ProviderObservable.getInstance().getCurrentProvider().supportsObfs4Kcp()) {
            RadioButton obfs4KcpRadioButton = new RadioButton(binding.getRoot().getContext());
            obfs4KcpRadioButton.setText(getText(R.string.tunnelling_obfs4_kcp));
            obfs4KcpRadioButton.setId(TUNNELING_OBFS4_KCP);
            obfs4KcpRadioButton.setChecked(getUseObfs4Kcp());
            binding.tunnelingRadioGroup.addView(obfs4KcpRadioButton);
        }

        binding.tunnelingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            useBridges(true);
            setUseTunnel(checkedId);
            tryReconnectVpn();
        });
    }

    private void initPortHopping() {
        binding.portHoppingSwitch.setVisibility(ProviderObservable.getInstance().getCurrentProvider().supportsObfs4Hop() ? View.VISIBLE : View.GONE);
        binding.portHoppingSwitch.findViewById(R.id.material_icon).setVisibility(View.GONE);
        binding.portHoppingSwitch.setChecked(getUsePortHopping());
        binding.portHoppingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            useBridges(true);
            setUsePortHopping(isChecked);
            tryReconnectVpn();
        });
    }
}