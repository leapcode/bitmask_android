package se.leap.bitmaskclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.AppCompatButton;

import butterknife.InjectView;
import butterknife.OnClick;
import se.leap.bitmaskclient.userstatus.SessionDialog;
import se.leap.bitmaskclient.userstatus.User;

/**
 * Base Activity for activities concerning a provider interaction
 *
 * Created by fupduck on 09.01.18.
 */

public abstract class ProviderCredentialsBaseActivity extends ConfigWizardBaseActivity {

    protected ProviderAPIResultReceiver providerAPIResultReceiver;

    @InjectView(R.id.provider_credentials_username)
    TextInputEditText providerCredentialsUsername;

    @InjectView(R.id.provider_credentials_password)
    TextInputEditText providerCredentialsPassword;

    @InjectView(R.id.button)
    AppCompatButton providerCredentialsButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        providerAPIResultReceiver = new ProviderAPIResultReceiver(new Handler(), new ProviderCredentialsReceiver(this));
    }

    @OnClick(R.id.button)
    abstract void handleButton();

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
        showProgressBar();
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        ProviderAPICommand.execute(parameters, ProviderAPI.LOG_IN, providerAPIResultReceiver);
    }

    void downloadVpnCertificate() {
        Intent providerAPICommand = new Intent(this, ProviderAPI.class);
        ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.DOWNLOAD_CERTIFICATE, providerAPIResultReceiver);
    }


    public void signUp(String username, String password) {
        showProgressBar();
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
                activity.downloadVpnCertificate();
            } else if (resultCode == ProviderAPI.FAILED_SIGNUP) {
                //MainActivity.sessionDialog(resultData);
            } else if (resultCode == ProviderAPI.SUCCESSFUL_LOGIN) {
                activity.downloadVpnCertificate();
            } else if (resultCode == ProviderAPI.FAILED_LOGIN) {
                //MainActivity.sessionDialog(resultData);
//  TODO MOVE
//          } else if (resultCode == ProviderAPI.SUCCESSFUL_LOGOUT) {
//                if (switching_provider) activity.switchProvider();
//            } else if (resultCode == ProviderAPI.LOGOUT_FAILED) {
//                activity.setResult(RESULT_CANCELED);
            } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
                //activity.eip_fragment.updateEipService();
            } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
                // TODO activity.setResult(RESULT_CANCELED);
//            } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE) {
//                activity.eip_fragment.updateEipService();
//                activity.setResult(RESULT_OK);
//            } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE) {
//                activity.setResult(RESULT_CANCELED);
            }
        }
    }

}
