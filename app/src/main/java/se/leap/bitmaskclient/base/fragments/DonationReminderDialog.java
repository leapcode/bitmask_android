package se.leap.bitmaskclient.base.fragments;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.text.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.DateHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static se.leap.bitmaskclient.base.models.Constants.DONATION_REMINDER_DURATION;
import static se.leap.bitmaskclient.base.models.Constants.DONATION_URL;
import static se.leap.bitmaskclient.base.models.Constants.ENABLE_DONATION;
import static se.leap.bitmaskclient.base.models.Constants.ENABLE_DONATION_REMINDER;
import static se.leap.bitmaskclient.base.models.Constants.FIRST_TIME_USER_DATE;
import static se.leap.bitmaskclient.base.models.Constants.LAST_DONATION_REMINDER_DATE;

public class DonationReminderDialog extends AppCompatDialogFragment {

    public final static String TAG = DonationReminderDialog.class.getName();
    private static boolean isShown = false;

    @BindView(R.id.btnDonate)
    Button btnDonate;

    @BindView(R.id.btnLater)
    Button btnLater;

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
        View view = inflater.inflate(R.layout.donation_reminder_dialog, null);
        unbinder = ButterKnife.bind(this, view);
        isShown = true;

        builder.setView(view);
        btnDonate.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL));
            try {
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
            PreferenceHelper.putString(getContext(), LAST_DONATION_REMINDER_DATE,
                    DateHelper.getCurrentDateString());
            dismiss();
        });
        btnLater.setOnClickListener(v -> {
            PreferenceHelper.putString(getContext(), LAST_DONATION_REMINDER_DATE,
                    DateHelper.getCurrentDateString());
            dismiss();
        });

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
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
        
        String firstTimeUserDate = PreferenceHelper.getString(context, FIRST_TIME_USER_DATE, null);
        if (firstTimeUserDate == null) {
            PreferenceHelper.putString(context, FIRST_TIME_USER_DATE, DateHelper.getCurrentDateString());
            return false;
        }

        try {
            long diffDays;

            diffDays = DateHelper.getDateDiffToCurrentDateInDays(firstTimeUserDate);
            if (diffDays < 1) {
                return false;
            }

            String lastDonationReminderDate = PreferenceHelper.getString(context, LAST_DONATION_REMINDER_DATE, null);
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
