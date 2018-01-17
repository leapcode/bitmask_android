package se.leap.bitmaskclient;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Create an account with a provider
 */

public class SignupActivity extends ProviderCredentialsBaseActivity {

    @InjectView(R.id.provider_credentials_password_verification)
    TextInputEditText providerCredentialsPasswordVerification;

    @InjectView(R.id.provider_credentials_password_verification_layout)
    TextInputLayout providerCredentialsPasswordVerificationLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_signup);

        setProviderHeaderText("providerNAME");
        setProviderHeaderLogo(R.drawable.mask);

        setButtonText(R.string.signup_button);

        providerCredentialsPasswordVerification.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(getPassword().equals(getPasswordVerification())) {
                    providerCredentialsPasswordVerificationLayout.setError(null);
                } else {
                    providerCredentialsPasswordVerificationLayout.setError(getString(R.string.password_mismatch));
                }
            }
        });
    }

    @Override
    @OnClick(R.id.button)
    void handleButton() {
        if (getPassword().equals(getPasswordVerification())) {
            signUp(getUsername(), getPassword());
        }
    }

    private String getPasswordVerification() {
        return providerCredentialsPasswordVerification.getText().toString();
    }
}
