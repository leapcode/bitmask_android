package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
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

import butterknife.InjectView;
import butterknife.OnClick;
import se.leap.bitmaskclient.userstatus.SessionDialog;
import se.leap.bitmaskclient.userstatus.SessionDialog.ERRORS;
import se.leap.bitmaskclient.userstatus.User;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.ProviderAPI.RESULT_CODE;
import static se.leap.bitmaskclient.ProviderAPI.RESULT_KEY;
import static se.leap.bitmaskclient.userstatus.SessionDialog.USERNAME;

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
    final private static String USER_MESSAGE = "USER_MESSAGE";
    final private static String USERNAME_ERROR = "USERNAME_ERROR";
    final private static String PASSWORD_ERROR = "PASSWORD_ERROR";
    final private static String PASSWORD_VERIFICATION_ERROR = "PASSWORD_VERIFICATION_ERROR";

    protected Intent mConfigState = new Intent(SHOWING_FORM);
    protected ProviderAPIBroadcastReceiver providerAPIBroadcastReceiver;

    @InjectView(R.id.provider_credentials_user_message)
    AppCompatTextView userMessage;

    @InjectView(R.id.provider_credentials_username)
    TextInputEditText usernameField;

    @InjectView(R.id.provider_credentials_password)
    TextInputEditText passwordField;

    @InjectView(R.id.provider_credentials_password_verification)
    TextInputEditText passwordVerificationField;

    @InjectView(R.id.provider_credentials_username_error)
    TextInputLayout usernameError;

    @InjectView(R.id.provider_credentials_password_error)
    TextInputLayout passwordError;

    @InjectView(R.id.provider_credentials_password_verification_error)
    TextInputLayout passwordVerificationError;

    @InjectView(R.id.button)
    AppCompatButton button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_provider_credentials);
        providerAPIBroadcastReceiver = new ProviderAPIBroadcastReceiver();

        IntentFilter updateIntentFilter = new IntentFilter(PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(providerAPIBroadcastReceiver, updateIntentFilter);

        setUpListeners();
        if(savedInstanceState != null) {
            restoreState(savedInstanceState);
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

    private void restoreState(Bundle savedInstance) {
        if (savedInstance.getString(USER_MESSAGE) != null) {
            userMessage.setText(savedInstance.getString(USER_MESSAGE));
            userMessage.setVisibility(VISIBLE);
        }
        usernameError.setError(savedInstance.getString(USERNAME_ERROR));
        passwordError.setError(savedInstance.getString(PASSWORD_ERROR));
        passwordVerificationError.setError(savedInstance.getString(PASSWORD_VERIFICATION_ERROR));
        if (savedInstance.getString(ACTIVITY_STATE) != null) {
            mConfigState.setAction(savedInstance.getString(ACTIVITY_STATE));
        }
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
            unregisterReceiver(providerAPIBroadcastReceiver);
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
            return username.split("@" + providerDomain)[0];
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
        User.setUserName(username);

        Intent providerAPICommand = new Intent(this, ProviderAPI.class);
        Bundle parameters = bundlePassword(password);
        providerAPICommand.setAction(ProviderAPI.LOG_IN);
        providerAPICommand.putExtra(ProviderAPI.PARAMETERS, parameters);
        startService(providerAPICommand);
    }

    public void signUp(String username, String password) {
        User.setUserName(username);

        Intent providerAPICommand = new Intent(this, ProviderAPI.class);
        Bundle parameters = bundlePassword(password);
        providerAPICommand.setAction(ProviderAPI.SIGN_UP);
        providerAPICommand.putExtra(ProviderAPI.PARAMETERS, parameters);
        startService(providerAPICommand);
    }

    void downloadVpnCertificate() {
        Intent providerAPICommand = new Intent(this, ProviderAPI.class);
        providerAPICommand.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
        providerAPICommand.putExtra(ProviderAPI.PARAMETERS, Bundle.EMPTY);
        startService(providerAPICommand);
    }

    protected Bundle bundlePassword(String password) {
        Bundle parameters = new Bundle();
        if (!password.isEmpty())
            parameters.putString(SessionDialog.PASSWORD, password);
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
                    usernameError.setError(getString(R.string.username_ask));
                } else {
                    usernameError.setError(null);
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
                    passwordError.setError(getString(R.string.error_not_valid_password_user_message));
                } else {
                    passwordError.setError(null);
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
                    passwordVerificationError.setError(null);
                } else {
                    passwordVerificationError.setError(getString(R.string.password_mismatch));
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
        if (arguments.containsKey(ERRORS.PASSWORD_INVALID_LENGTH.toString())) {
            passwordError.setError(getString(R.string.error_not_valid_password_user_message));
        } else if (arguments.containsKey(ERRORS.RISEUP_WARNING.toString())) {
            userMessage.setVisibility(VISIBLE);
            userMessage.setText(R.string.login_riseup_warning);
        }
        if (arguments.containsKey(USERNAME)) {
            String username = arguments.getString(USERNAME);
            usernameField.setText(username);
        }
        if (arguments.containsKey(ERRORS.USERNAME_MISSING.toString())) {
            usernameError.setError(getString(R.string.username_ask));
        }
        if (arguments.containsKey(getString(R.string.user_message))) {
            String userMessageString = arguments.getString(getString(R.string.user_message));
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
        } else if (userMessage.getVisibility() != VISIBLE) {
            userMessage.setVisibility(GONE);
        }

        if (!usernameField.getText().toString().isEmpty() && passwordField.isFocusable())
            passwordField.requestFocus();

        mConfigState.setAction(SHOWING_FORM);
        hideProgressBar();
    }

    private void successfullyFinished() {
        Intent resultData = new Intent();
        resultData.putExtra(Provider.KEY, provider);
        setResult(RESULT_OK, resultData);
        finish();
    }

    public class ProviderAPIBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received Broadcast");

            String action = intent.getAction();
            if (action == null || !action.equalsIgnoreCase(PROVIDER_API_EVENT)) {
                return;
            }

            int resultCode = intent.getIntExtra(RESULT_CODE, -1);
            switch (resultCode) {
                case ProviderAPI.SUCCESSFUL_SIGNUP:
                case ProviderAPI.SUCCESSFUL_LOGIN:
                    downloadVpnCertificate();
                    break;
                case ProviderAPI.FAILED_LOGIN:
                case ProviderAPI.FAILED_SIGNUP:
                    handleReceivedErrors((Bundle) intent.getParcelableExtra(RESULT_KEY));
                    break;

                case ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE:
                    successfullyFinished();
                    //activity.eip_fragment.updateEipService();
                    break;
                case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE:
                    // TODO activity.setResult(RESULT_CANCELED);
                    break;
            }
        }
    }
}
