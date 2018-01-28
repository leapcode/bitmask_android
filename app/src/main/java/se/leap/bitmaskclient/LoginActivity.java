package se.leap.bitmaskclient;

import android.os.Bundle;
import android.support.annotation.Nullable;

import butterknife.OnClick;

/**
 * Activity to login to chosen Provider
 *
 * Created by fupduck on 09.01.18.
 */

public class LoginActivity extends ProviderCredentialsBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProgressbarText(R.string.logging_in);
        setProviderHeaderLogo(R.drawable.mask);
        setProviderHeaderText(R.string.login_to_profile);
    }

    @Override
    @OnClick(R.id.button)
    void handleButton() {
        super.handleButton();
        login(getUsername(), getPassword());
    }

}
