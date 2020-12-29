package se.leap.bitmaskclient.providersetup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import butterknife.BindView;
import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.providersetup.activities.ProviderListBaseActivity.EXTRAS_KEY_INVALID_URL;

/**
 * Created by cyberta on 30.06.18.
 */

public abstract class AddProviderBaseActivity extends ConfigWizardBaseActivity {

    final public static String EXTRAS_KEY_NEW_URL = "NEW_URL";

    @BindView(R.id.text_uri_error)
    TextInputLayout urlError;

    @BindView(R.id.text_uri)
    TextInputEditText editUrl;

    @BindView(R.id.button_cancel)
    Button cancelButton;

    @BindView(R.id.button_save)
    Button saveButton;


    protected void init() {
        Bundle extras = this.getIntent().getExtras();
        if (extras != null && extras.containsKey(EXTRAS_KEY_INVALID_URL)) {
            editUrl.setText(extras.getString(EXTRAS_KEY_INVALID_URL));
            saveButton.setEnabled(true);
        }

        setupSaveButton();
        setupCancelButton();
        setUpListeners();
        setUpInitialUI();
    }

    public abstract void setupSaveButton();

    private void setupCancelButton() {
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setUpInitialUI() {
        setProviderHeaderText(R.string.add_provider);
        hideProgressBar();
    }

    protected void saveProvider() {
        String entered_url = getURL();
        if (validURL(entered_url)) {
            Intent intent = this.getIntent();
            intent.putExtra(EXTRAS_KEY_NEW_URL, entered_url);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            editUrl.setText("");
            urlError.setError(getString(R.string.not_valid_url_entered));
        }
    }

    private void setUpListeners() {

        editUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!validURL(getURL())) {
                    urlError.setError(getString(R.string.not_valid_url_entered));
                    saveButton.setEnabled(false);

                } else {
                    urlError.setError(null);
                    saveButton.setEnabled(true);
                }
            }
        });
    }

    private String getURL() {
        String entered_url = editUrl.getText().toString().trim();
        if (entered_url.contains("www.")) entered_url = entered_url.replaceFirst("www.", "");
        if (!entered_url.startsWith("https://")) {
            if (entered_url.startsWith("http://")) {
                entered_url = entered_url.substring("http://".length());
            }
            entered_url = "https://".concat(entered_url);
        }
        return entered_url;
    }

    /**
     * Checks if the entered url is valid or not.
     *
     * @param enteredUrl
     * @return true if it's not empty nor contains only the protocol.
     */
    boolean validURL(String enteredUrl) {
        return android.util.Patterns.WEB_URL.matcher(enteredUrl).matches();
    }
}
