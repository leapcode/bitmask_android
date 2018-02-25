package se.leap.bitmaskclient.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.ConfigHelper.saveShowAlwaysOnDialog;

/**
 * Created by cyberta on 25.02.18.
 */



public class AlwaysOnDialog extends AppCompatDialogFragment {

    public final static String TAG = AlwaysOnDialog.class.getName();

    @InjectView(R.id.do_not_show_again)
    CheckBox doNotShowAgainCheckBox;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.checkbox_confirm_dialog, null);
        ButterKnife.inject(this, view);

        builder.setView(view)
                .setMessage(R.string.always_on_vpn_user_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (doNotShowAgainCheckBox.isChecked()) {
                            saveShowAlwaysOnDialog(getContext(), false);
                        }
                        Intent intent = new Intent("android.net.vpn.SETTINGS");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
