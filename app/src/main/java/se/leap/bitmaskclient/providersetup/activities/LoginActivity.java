package se.leap.bitmaskclient.providersetup.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;

import butterknife.OnClick;
import se.leap.bitmaskclient.R;

/**
 * Activity to login to chosen Provider
 *
 * Created by fupduck on 09.01.18.
 */

public class LoginActivity extends ProviderCredentialsBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProgressbarTitle(R.string.logging_in);
        setProviderHeaderLogo(R.drawable.logo);
        setProviderHeaderText(R.string.login_to_profile);
    }

    @Override
    @OnClick(R.id.button)
    void handleButton() {
        super.handleButton();
        login(getUsername(), getPassword());
    }

}
