package se.leap.bitmaskclient.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.views.IconTextView;

import static se.leap.bitmaskclient.utils.PreferenceHelper.saveShowAlwaysOnDialog;


/**
 * Created by cyberta on 25.02.18.
 */



public class AlwaysOnDialog extends AppCompatDialogFragment {

    public final static String TAG = AlwaysOnDialog.class.getName();

    @InjectView(R.id.do_not_show_again)
    CheckBox doNotShowAgainCheckBox;

    @InjectView(R.id.user_message)
    IconTextView userMessage;

    @InjectView(R.id.block_vpn_user_message)
    AppCompatTextView blockVpnUserMessage;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.d_checkbox_confirm, null);
        ButterKnife.inject(this, view);

        userMessage.setIcon(R.drawable.ic_settings);
        userMessage.setText(getString(R.string.always_on_vpn_user_message));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            blockVpnUserMessage.setVisibility(View.VISIBLE);
        }

        builder.setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (doNotShowAgainCheckBox.isChecked()) {
                        saveShowAlwaysOnDialog(getContext(), false);
                    }
                    Intent intent = new Intent("android.net.vpn.SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        return builder.create();
    }
}
