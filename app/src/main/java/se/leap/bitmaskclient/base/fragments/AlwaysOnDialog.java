package se.leap.bitmaskclient.base.fragments;

import static se.leap.bitmaskclient.base.utils.PreferenceHelper.saveShowAlwaysOnDialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.DCheckboxConfirmBinding;


/**
 * Created by cyberta on 25.02.18.
 */



public class AlwaysOnDialog extends AppCompatDialogFragment {

    public final static String TAG = AlwaysOnDialog.class.getName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        DCheckboxConfirmBinding binding = DCheckboxConfirmBinding.inflate(inflater);

        binding.userMessage.setIcon(R.drawable.ic_settings);
        binding.userMessage.setText(getString(R.string.always_on_vpn_user_message));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.blockVpnUserMessage.setVisibility(View.VISIBLE);
        }

        builder.setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (binding.doNotShowAgain.isChecked()) {
                        saveShowAlwaysOnDialog(false);
                    }
                    Intent intent = new Intent("android.net.vpn.SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        return builder.create();
    }

}
