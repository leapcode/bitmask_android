package se.leap.bitmaskclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import javax.inject.Inject;

import butterknife.InjectView;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;

public class AddProviderActivity extends ConfigWizardBaseActivity {

    final public static String TAG = "AddProviderActivity";
    final public static String EXTRAS_KEY_NEW_URL = "NEW_URL";

    @InjectView(R.id.text_uri_error)
    TextInputLayout urlError;

    @InjectView(R.id.text_uri)
    TextInputEditText editUrl;

    @InjectView(R.id.danger_checkbox)
    CheckBox checkboxDanger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.a_add_provider);
        if (this.getIntent().getExtras() != null) {
            editUrl.setText(this.getIntent().getExtras().getString(ProviderListBaseActivity.EXTRAS_KEY_INVALID_URL));
        }

        checkboxDanger.setVisibility(View.VISIBLE);
        checkboxDanger.setText(R.string.danger_checkbox);
        checkboxDanger.setChecked(preferences.getBoolean(ProviderItem.DANGER_ON, false));

        final Button saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                preferences.edit().putBoolean(ProviderItem.DANGER_ON, checkboxDanger.isChecked()).apply();
                saveProvider();
            }
        });

        final Button cancelButton = findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        setUpListeners();
        setUpInitialUI();
    }

    private void setUpInitialUI() {
        setProviderHeaderText(R.string.add_provider);
        hideProgressBar();
    }

    private void saveProvider() {
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
                } else {
                    urlError.setError(null);
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
