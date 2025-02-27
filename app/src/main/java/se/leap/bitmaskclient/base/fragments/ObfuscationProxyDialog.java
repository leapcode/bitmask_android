package se.leap.bitmaskclient.base.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.blinkt.openvpn.core.connection.Connection.TransportProtocol.KCP;
import static de.blinkt.openvpn.core.connection.Connection.TransportProtocol.QUIC;
import static de.blinkt.openvpn.core.connection.Connection.TransportProtocol.TCP;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;

import se.leap.bitmaskclient.base.utils.BuildConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.DObfuscationProxyBinding;
import se.leap.bitmaskclient.eip.GatewaysManager;

public class ObfuscationProxyDialog extends AppCompatDialogFragment {
    public static final String TAG = ObfuscationProxyDialog.class.getSimpleName();
    DObfuscationProxyBinding binding;
    AppCompatEditText ipField;
    AppCompatEditText portField;
    AppCompatEditText certificateField;
    AppCompatButton saveButton;
    AppCompatButton useDefaultsButton;
    AppCompatButton cancelButton;
    AppCompatSpinner protocolSpinner;
    private final String[] protocols = { TCP.toString(), KCP.toString(), QUIC.toString() };


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DObfuscationProxyBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        ipField = binding.ipField;
        portField = binding.portField;
        certificateField = binding.certField;
        saveButton = binding.buttonSave;
        useDefaultsButton = binding.buttonDefaults;
        cancelButton = binding.buttonCancel;
        protocolSpinner = binding.protocolSpinner;

        ipField.setText(PreferenceHelper.getObfuscationPinningIP());
        portField.setText(PreferenceHelper.getObfuscationPinningPort());
        certificateField.setText(PreferenceHelper.getObfuscationPinningCert());

        GatewaysManager gatewaysManager = new GatewaysManager(getContext());


        ArrayAdapter<String> adapter = new ArrayAdapter<>(binding.getRoot().getContext(), android.R.layout.simple_spinner_item, protocols);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protocolSpinner.setAdapter(adapter);

        protocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferenceHelper.setObfuscationPinningProtocol(protocols[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        saveButton.setOnClickListener(v -> {
            String ip = TextUtils.isEmpty(ipField.getText()) ? null : ipField.getText().toString();
            PreferenceHelper.setObfuscationPinningIP(ip);
            String port = TextUtils.isEmpty(portField.getText()) ? null : portField.getText().toString();
            PreferenceHelper.setObfuscationPinningPort(port);
            String cert = TextUtils.isEmpty(certificateField.getText()) ? null : certificateField.getText().toString();
            PreferenceHelper.setObfuscationPinningCert(cert);
            PreferenceHelper.setUseObfuscationPinning(ip != null && port != null && cert != null);
            PreferenceHelper.setObfuscationPinningGatewayLocation(gatewaysManager.getLocationNameForIP(ip, v.getContext()));
            dismiss();
        });

        useDefaultsButton.setVisibility(BuildConfigHelper.hasObfuscationPinningDefaults() ? VISIBLE : GONE);
        useDefaultsButton.setOnClickListener(v -> {
           ipField.setText(BuildConfigHelper.obfsvpnIP());
           portField.setText(BuildConfigHelper.obfsvpnPort());
           certificateField.setText(BuildConfigHelper.obfsvpnCert());
           protocolSpinner.setSelection(getIndexForProtocol(BuildConfigHelper.obfsvpnTransportProtocol()));
        });

        cancelButton.setOnClickListener(v -> {
            boolean allowPinning = !TextUtils.isEmpty(ipField.getText()) && !TextUtils.isEmpty(portField.getText()) && !TextUtils.isEmpty(certificateField.getText());
            PreferenceHelper.setUseObfuscationPinning(allowPinning);
            dismiss();
        });

        return builder.create();
    }

    private int getIndexForProtocol(String protocol) {
        for (int i = 0; i < protocols.length; i++) {
            if (protocols[i].equals(protocol)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
