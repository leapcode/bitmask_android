/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.providersetup.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import butterknife.BindView;
import butterknife.OnClick;
import se.leap.bitmaskclient.base.models.Constants.CREDENTIAL_ERRORS;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.ProviderAPI;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.R;

import static android.text.TextUtils.isEmpty;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.CREDENTIALS_PASSWORD;
import static se.leap.bitmaskclient.base.models.Constants.CREDENTIALS_USERNAME;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.LOG_IN;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SIGN_UP;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.USER_MESSAGE;

/**
 * Base Activity for activities concerning a provider interaction
 *
 * Created by fupduck on 09.01.18.
 */

public abstract class ProviderCredentialsBaseActivity extends ConfigWizardBaseActivity {

    final protected static String TAG = ProviderCredentialsBaseActivity.class.getName();

    final private static String ACTIVITY_STATE = "ACTIVITY STATE";

    final private static String SHOWING_FORM = "SHOWING_FORM";
    final private static String PERFORMING_ACTION = "PERFORMING_ACTION";
    final private static String USERNAME_ERROR = "USERNAME_ERROR";
    final private static String PASSWORD_ERROR = "PASSWORD_ERROR";
    final private static String PASSWORD_VERIFICATION_ERROR = "PASSWORD_VERIFICATION_ERROR";

    protected Intent mConfigState = new Intent(SHOWING_FORM);
    protected ProviderAPIBroadcastReceiver providerAPIBroadcastReceiver;

    @BindView(R.id.provider_credentials_user_message)
    AppCompatTextView userMessage;

    @BindView(R.id.provider_credentials_username)
    TextInputEditText usernameField;

    @BindView(R.id.provider_credentials_password)
    TextInputEditText passwordField;

    @BindView(R.id.provider_credentials_password_verification)
    TextInputEditText passwordVerificationField;

    @BindView(R.id.provider_credentials_username_error)
    TextInputLayout usernameError;

    @BindView(R.id.provider_credentials_password_error)
    TextInputLayout passwordError;

    @BindView(R.id.provider_credentials_password_verification_error)
    TextInputLayout passwordVerificationError;

    @BindView(R.id.button)
    AppCompatButton button;

    private boolean isUsernameError = false;
    private boolean isPasswordError = false;
    private boolean isVerificationError = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_provider_credentials);
        providerAPIBroadcastReceiver = new ProviderAPIBroadcastReceiver();

        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(providerAPIBroadcastReceiver, updateIntentFilter);

        setUpListeners();
        restoreState(savedInstanceState);

        String userMessageString = getIntent().getStringExtra(USER_MESSAGE);
        if (userMessageString != null) {
            userMessage.setText(userMessageString);
            userMessage.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String action = mConfigState.getAction();
        if (action == null) {
            return;
        }

        if(action.equalsIgnoreCase(PERFORMING_ACTION)) {
            showProgressBar();
        }
    }

    protected void restoreState(Bundle savedInstance) {
        super.restoreState(savedInstance);
        if (savedInstance == null) {
            return;
        }
        if (savedInstance.getString(USER_MESSAGE) != null) {
            userMessage.setText(savedInstance.getString(USER_MESSAGE));
            userMessage.setVisibility(VISIBLE);
        }
        updateUsernameError(savedInstance.getString(USERNAME_ERROR));
        updatePasswordError(savedInstance.getString(PASSWORD_ERROR));
        updateVerificationError(savedInstance.getString(PASSWORD_VERIFICATION_ERROR));
        if (savedInstance.getString(ACTIVITY_STATE) != null) {
            mConfigState.setAction(savedInstance.getString(ACTIVITY_STATE));
        }
    }

    private void updateUsernameError(String usernameErrorString) {
        usernameError.setError(usernameErrorString);
        isUsernameError = usernameErrorString != null;
        updateButton();
    }

    private void updatePasswordError(String passwordErrorString) {
        passwordError.setError(passwordErrorString);
        isPasswordError = passwordErrorString != null;
        updateButton();
    }

    private void updateVerificationError(String verificationErrorString) {
        passwordVerificationError.setError(verificationErrorString);
        isVerificationError = verificationErrorString != null;
        updateButton();
    }

    private void updateButton() {
        button.setEnabled(!isPasswordError &&
                !isUsernameError &&
                !isVerificationError &&
                !isEmpty(passwordField.getText()) &&
                !isEmpty(usernameField.getText()) &&
                !(passwordVerificationField.getVisibility() == VISIBLE &&
                getPasswordVerification().length() == 0));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ACTIVITY_STATE, mConfigState.getAction());
        if (userMessage.getText() != null && userMessage.getVisibility() == VISIBLE) {
            outState.putString(USER_MESSAGE, userMessage.getText().toString());
        }
        if (usernameError.getError() != null) {
            outState.putString(USERNAME_ERROR, usernameError.getError().toString());
        }
        if (passwordError.getError() != null) {
            outState.putString(PASSWORD_ERROR, passwordError.getError().toString());
        }
        if (passwordVerificationError.getError() != null) {
            outState.putString(PASSWORD_VERIFICATION_ERROR, passwordVerificationError.getError().toString());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (providerAPIBroadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(providerAPIBroadcastReceiver);
    }

    @OnClick(R.id.button)
    void handleButton() {
        mConfigState.setAction(PERFORMING_ACTION);
        hideKeyboard();
        showProgressBar();
    }

    protected void setButtonText(@StringRes int buttonText) {
        button.setText(buttonText);
    }

    String getUsername() {
        String username = usernameField.getText().toString();
        String providerDomain = provider.getDomain();
        if (username.endsWith(providerDomain)) {
            try {
                return username.split("@" + providerDomain)[0];
            } catch (ArrayIndexOutOfBoundsException e) {
                return "";
            }
        }
        return username;
    }

    String getPassword() {
        return passwordField.getText().toString();
    }

    String getPasswordVerification() {
        return passwordVerificationField.getText().toString();
    }

    void login(String username, String password) {

        Bundle parameters = bundleUsernameAndPassword(username, password);
        ProviderAPICommand.execute(this, LOG_IN, parameters, provider);
    }

    public void signUp(String username, String password) {

        Bundle parameters = bundleUsernameAndPassword(username, password);
        ProviderAPICommand.execute(this, SIGN_UP, parameters, provider);
    }

    void downloadVpnCertificate(Provider handledProvider) {
        provider = handledProvider;
        ProviderAPICommand.execute(this, DOWNLOAD_VPN_CERTIFICATE, provider);
    }

    protected Bundle bundleUsernameAndPassword(String username, String password) {
        Bundle parameters = new Bundle();
        if (!username.isEmpty())
            parameters.putString(CREDENTIALS_USERNAME, username);
        if (!password.isEmpty())
            parameters.putString(CREDENTIALS_PASSWORD, password);
        return parameters;
    }

    private void setUpListeners() {
        usernameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (getUsername().equalsIgnoreCase("")) {
                    s.clear();
                    updateUsernameError(getString(R.string.username_ask));
                } else {
                    updateUsernameError(null);
                    String suffix = "@" + provider.getDomain();
                    if (!usernameField.getText().toString().endsWith(suffix)) {
                        s.append(suffix);
                        usernameField.setSelection(usernameField.getText().toString().indexOf('@'));
                    }
                }
            }
        });
        usernameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == IME_ACTION_DONE
                        || event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    passwordField.requestFocus();
                    return true;
                }
                return false;
            }
        });

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(getPassword().length() < 8) {
                    updatePasswordError(getString(R.string.error_not_valid_password_user_message));
                } else {
                    updatePasswordError(null);
                }
            }
        });
        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == IME_ACTION_DONE
                        || event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (passwordVerificationField.getVisibility() == VISIBLE) {
                        passwordVerificationField.requestFocus();
                    } else {
                        button.performClick();
                    }
                    return true;
                }
                return false;
            }
        });

        passwordVerificationField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(getPassword().equals(getPasswordVerification())) {
                    updateVerificationError(null);
                } else {
                    updateVerificationError(getString(R.string.password_mismatch));
                }
            }
        });
        passwordVerificationField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == IME_ACTION_DONE
                        || event != null &&  event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    button.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(passwordField.getWindowToken(), 0);
        }
    }

    private void handleReceivedErrors(Bundle arguments) {
        if (arguments.containsKey(CREDENTIAL_ERRORS.PASSWORD_INVALID_LENGTH.toString())) {
            updatePasswordError(getString(R.string.error_not_valid_password_user_message));
        } else if (arguments.containsKey(CREDENTIAL_ERRORS.RISEUP_WARNING.toString())) {
            userMessage.setVisibility(VISIBLE);
            userMessage.setText(R.string.login_riseup_warning);
        }
        if (arguments.containsKey(CREDENTIALS_USERNAME)) {
            String username = arguments.getString(CREDENTIALS_USERNAME);
            usernameField.setText(username);
        }
        if (arguments.containsKey(CREDENTIAL_ERRORS.USERNAME_MISSING.toString())) {
            updateUsernameError(getString(R.string.username_ask));
        }
        if (arguments.containsKey(USER_MESSAGE)) {
            String userMessageString = arguments.getString(USER_MESSAGE);
            try {
                 userMessageString = new JSONArray(userMessageString).getString(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                userMessage.setText(Html.fromHtml(userMessageString, Html.FROM_HTML_MODE_LEGACY));
            } else {
                userMessage.setText(Html.fromHtml(userMessageString));
            }
            Linkify.addLinks(userMessage, Linkify.ALL);
            userMessage.setMovementMethod(LinkMovementMethod.getInstance());
            userMessage.setVisibility(VISIBLE);
        } else if (userMessage.getVisibility() != GONE) {
            userMessage.setVisibility(GONE);
        }

        if (!usernameField.getText().toString().isEmpty() && passwordField.isFocusable())
            passwordField.requestFocus();

        mConfigState.setAction(SHOWING_FORM);
        hideProgressBar();
    }

    private void successfullyFinished(Provider handledProvider) {
        provider = handledProvider;
        Intent resultData = new Intent();
        resultData.putExtra(Provider.KEY, provider);
        setResult(RESULT_OK, resultData);
        finish();
    }

    //TODO: replace with EipSetupObserver
    public class ProviderAPIBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received Broadcast");

            String action = intent.getAction();
            if (action == null || !action.equalsIgnoreCase(BROADCAST_PROVIDER_API_EVENT)) {
                return;
            }

            int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
            Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
            Provider handledProvider = resultData.getParcelable(PROVIDER_KEY);

            switch (resultCode) {
                case ProviderAPI.SUCCESSFUL_SIGNUP:
                    String password = resultData.getString(CREDENTIALS_PASSWORD);
                    String username = resultData.getString(CREDENTIALS_USERNAME);
                    login(username, password);
                    break;
                case ProviderAPI.SUCCESSFUL_LOGIN:
                    downloadVpnCertificate(handledProvider);
                    break;
                case ProviderAPI.FAILED_LOGIN:
                case ProviderAPI.FAILED_SIGNUP:
                    handleReceivedErrors((Bundle) intent.getParcelableExtra(BROADCAST_RESULT_KEY));
                    break;

                case ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                    // error handling takes place in MainActivity
                case ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                    successfullyFinished(handledProvider);
                    break;
            }
        }
    }
}
