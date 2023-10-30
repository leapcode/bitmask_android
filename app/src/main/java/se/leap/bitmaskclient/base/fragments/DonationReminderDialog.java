package se.leap.bitmaskclient.base.fragments;

import static se.leap.bitmaskclient.base.models.Constants.DONATION_REMINDER_DURATION;
import static se.leap.bitmaskclient.base.models.Constants.DONATION_URL;
import static se.leap.bitmaskclient.base.models.Constants.ENABLE_DONATION;
import static se.leap.bitmaskclient.base.models.Constants.ENABLE_DONATION_REMINDER;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.text.ParseException;

import se.leap.bitmaskclient.base.utils.DateHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.DonationReminderDialogBinding;

public class DonationReminderDialog extends AppCompatDialogFragment {

    public final static String TAG = DonationReminderDialog.class.getName();
    private static boolean isShown = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        DonationReminderDialogBinding binding = DonationReminderDialogBinding.inflate(inflater);
        isShown = true;

        builder.setView(binding.getRoot());
        binding.btnDonate.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL));
            try {
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
            PreferenceHelper.lastDonationReminderDate(DateHelper.getCurrentDateString());
            dismiss();
        });
        binding.btnLater.setOnClickListener(v -> {
            PreferenceHelper.lastDonationReminderDate(DateHelper.getCurrentDateString());
            dismiss();
        });

        return builder.create();
    }

    public static boolean isCallable(Context context) {
        if (isShown) {
            return false;
        }

        if (!ENABLE_DONATION || !ENABLE_DONATION_REMINDER) {
            return false;
        }

        if (context == null) {
            Log.e(TAG, "context is null!");
            return false;
        }
        String firstTimeUserDate = PreferenceHelper.getFirstTimeUserDate();
        if (firstTimeUserDate == null) {
            PreferenceHelper.firstTimeUserDate(DateHelper.getCurrentDateString());
            return false;
        }

        try {
            long diffDays;

            diffDays = DateHelper.getDateDiffToCurrentDateInDays(firstTimeUserDate);
            if (diffDays < 1) {
                return false;
            }

            String lastDonationReminderDate = PreferenceHelper.getLastDonationReminderDate();
            if (lastDonationReminderDate == null) {
                return true;
            }
            diffDays = DateHelper.getDateDiffToCurrentDateInDays(lastDonationReminderDate);
            return diffDays >= DONATION_REMINDER_DURATION;

        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            return false;
        }
    }
}
