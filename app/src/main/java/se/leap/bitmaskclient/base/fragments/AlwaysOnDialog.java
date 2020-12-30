package se.leap.bitmaskclient.base.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.AppCompatTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.views.IconTextView;

import static se.leap.bitmaskclient.base.utils.PreferenceHelper.saveShowAlwaysOnDialog;


/**
 * Created by cyberta on 25.02.18.
 */



public class AlwaysOnDialog extends AppCompatDialogFragment {

    public final static String TAG = AlwaysOnDialog.class.getName();

    @BindView(R.id.do_not_show_again)
    CheckBox doNotShowAgainCheckBox;

    @BindView(R.id.user_message)
    IconTextView userMessage;

    @BindView(R.id.block_vpn_user_message)
    AppCompatTextView blockVpnUserMessage;

    private Unbinder unbinder;


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
        unbinder = ButterKnife.bind(this, view);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
