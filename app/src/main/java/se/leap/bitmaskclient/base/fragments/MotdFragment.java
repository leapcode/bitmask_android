package se.leap.bitmaskclient.base.fragments;

import static se.leap.bitmaskclient.base.MainActivity.ACTION_SHOW_VPN_FRAGMENT;
import static se.leap.bitmaskclient.base.models.Constants.EXTRA_MOTD_MSG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import de.blinkt.openvpn.core.VpnStatus;
import motd.IMessage;
import motd.Motd;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.databinding.FMotdBinding;


public class MotdFragment extends Fragment {

    private static final String TAG = MotdFragment.class.getSimpleName();
    private IMessage message;
    FMotdBinding binding;
    AppCompatTextView messageView;
    AppCompatImageButton nextButton;

    public MotdFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String messageString = getArguments().getString(EXTRA_MOTD_MSG);
            if (messageString != null) {
                Log.d(TAG, "MotdFragment received: " + messageString);
                message = Motd.newMessage(messageString);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FMotdBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messageView = binding.motdContent;
        nextButton = binding.nextBtn;
        String currentLang = Locale.getDefault().getLanguage();
        String text = message.getLocalizedText(currentLang);
        if (TextUtils.isEmpty(text)) {
            text = message.getLocalizedText("en");
        }

        if (TextUtils.isEmpty(text)) {
            String error = "Message of the day cannot be shown. Unsupported app language and unknown default langauge.";
            Log.e(TAG, error);
            VpnStatus.logError(error);
            showVpnFragment(view.getContext());
            return;
        }

        Log.d(TAG, "set motd text: " + text);
        messageView.setText(Html.fromHtml(text));
        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        nextButton.setOnClickListener(v -> {
                showVpnFragment(v.getContext());
        });

    }

    private void showVpnFragment(Context context) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_SHOW_VPN_FRAGMENT);
            context.startActivity(intent);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

    }
}