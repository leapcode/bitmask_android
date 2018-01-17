package se.leap.bitmaskclient;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import butterknife.OnClick;

/**
 * Created by fupduck on 09.01.18.
 */

public class LoginActivity extends ProviderCredentialsBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_login);

        setProviderHeaderText("providerNAME");
        setProviderHeaderLogo(R.drawable.mask);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    @OnClick(R.id.button)
    void handleButton() {
        login(getUsername(), getPassword());
    }

}
