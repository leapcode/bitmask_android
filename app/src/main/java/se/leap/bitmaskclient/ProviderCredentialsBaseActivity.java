package se.leap.bitmaskclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.InjectView;
import butterknife.OnClick;
import se.leap.bitmaskclient.userstatus.SessionDialog;
import se.leap.bitmaskclient.userstatus.User;

/**
 * Created by fupduck on 09.01.18.
 */

public abstract class ProviderCredentialsBaseActivity extends ButterKnifeActivity {

    protected ProviderAPIResultReceiver providerAPIResultReceiver;

    @InjectView(R.id.provider_header_logo)
    ImageView providerHeaderLogo;

    @InjectView(R.id.provider_header_text)
    TextView providerHeaderText;

    @InjectView(R.id.provider_credentials_username)
    TextInputEditText providerCredentialsUsername;

    @InjectView(R.id.provider_credentials_password)
    TextInputEditText providerCredentialsPassword;

    @InjectView(R.id.button)
    Button providerCredentialsButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        providerAPIResultReceiver = new ProviderAPIResultReceiver(new Handler(), new ProviderCredentialsReceiver(this));

    }

    @OnClick(R.id.button)
    abstract void handleButton();

    protected void setProviderHeaderLogo(@DrawableRes int providerHeaderLogo) {
        this.providerHeaderLogo.setImageResource(providerHeaderLogo);
    }

    protected void setProviderHeaderText(String providerHeaderText) {
        this.providerHeaderText.setText(providerHeaderText);
    }

    protected void setProviderHeaderText(@StringRes int providerHeaderText) {
        this.providerHeaderText.setText(providerHeaderText);
    }

    protected void setButtonText(@StringRes int buttonText) {
        providerCredentialsButton.setText(buttonText);
    }

    String getUsername() {
        return providerCredentialsUsername.getText().toString();
    }

    String getPassword() {
        return providerCredentialsPassword.getText().toString();
    }

    void login(String username, String password) {
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        ProviderAPICommand.execute(parameters, ProviderAPI.LOG_IN, providerAPIResultReceiver);
    }

    public void signUp(String username, String password) {
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        ProviderAPICommand.execute(parameters, ProviderAPI.SIGN_UP, providerAPIResultReceiver);
    }
    protected Bundle bundlePassword(String password) {
        Bundle parameters = new Bundle();
        if (!password.isEmpty())
            parameters.putString(SessionDialog.PASSWORD, password);
        return parameters;
    }

    public static class ProviderCredentialsReceiver implements ProviderAPIResultReceiver.Receiver{

        private ProviderCredentialsBaseActivity activity;

        ProviderCredentialsReceiver(ProviderCredentialsBaseActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == ProviderAPI.SUCCESSFUL_SIGNUP) {
                String username = resultData.getString(SessionDialog.USERNAME);
                String password = resultData.getString(SessionDialog.PASSWORD);
                activity.login(username, password);
            } else if (resultCode == ProviderAPI.FAILED_SIGNUP) {
                //MainActivity.sessionDialog(resultData);
            } else if (resultCode == ProviderAPI.SUCCESSFUL_LOGIN) {
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
            } else if (resultCode == ProviderAPI.FAILED_LOGIN) {
                //MainActivity.sessionDialog(resultData);
//  TODO MOVE
//          } else if (resultCode == ProviderAPI.SUCCESSFUL_LOGOUT) {
//                if (switching_provider) activity.switchProvider();
//            } else if (resultCode == ProviderAPI.LOGOUT_FAILED) {
//                activity.setResult(RESULT_CANCELED);
//            } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
//                activity.eip_fragment.updateEipService();
//                activity.setResult(RESULT_OK);
//            } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
//                activity.setResult(RESULT_CANCELED);
//            } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE) {
//                activity.eip_fragment.updateEipService();
//                activity.setResult(RESULT_OK);
//            } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE) {
//                activity.setResult(RESULT_CANCELED);
            }
        }
    }

}
