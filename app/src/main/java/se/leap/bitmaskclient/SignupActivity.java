package se.leap.bitmaskclient;

import android.os.Bundle;
import android.support.annotation.Nullable;

import butterknife.OnClick;

/**
 * Create an account with a provider
 */

public class SignupActivity extends ProviderCredentialsBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setProviderHeaderLogo(R.drawable.mask);
        setProviderHeaderText(R.string.create_profile);

        setProgressbarText(R.string.signing_up);
        setButtonText(R.string.signup_button);
    }

    @Override
    @OnClick(R.id.button)
    void handleButton() {
        super.handleButton();
        if (getPassword().equals(getPasswordVerification())) {
            signUp(getUsername(), getPassword());
        }
    }
}
