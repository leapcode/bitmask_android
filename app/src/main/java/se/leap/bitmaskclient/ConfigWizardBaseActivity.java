package se.leap.bitmaskclient;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.InjectView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

/**
 * Base Activity for configuration wizard activities
 *
 * Created by fupduck on 09.01.18.
 */

public abstract class ConfigWizardBaseActivity extends ButterKnifeActivity {

    protected SharedPreferences preferences;

    @InjectView(R.id.provider_header_logo)
    AppCompatImageView providerHeaderLogo;

    @InjectView(R.id.provider_header_text)
    AppCompatTextView providerHeaderText;

    @InjectView(R.id.loading_screen)
    protected LinearLayout loadingScreen;

    @InjectView(R.id.progressbar_description)
    protected AppCompatTextView progressbarText;

    @InjectView(R.id.content)
    protected LinearLayout content;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setProviderHeaderText(getProviderName());
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setProviderHeaderText(getProviderName());
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setProviderHeaderText(getProviderName());
    }

    protected void setProviderHeaderLogo(@DrawableRes int providerHeaderLogo) {
        this.providerHeaderLogo.setImageResource(providerHeaderLogo);
    }

    protected void setProviderHeaderText(String providerHeaderText) {
        this.providerHeaderText.setText(providerHeaderText);
    }

    protected void setProviderHeaderText(@StringRes int providerHeaderText) {
        this.providerHeaderText.setText(providerHeaderText);
    }

    protected String getProviderName() {
        try {
            JSONObject providerJson = new JSONObject(preferences.getString(Provider.KEY, ""));
            return providerJson.getJSONObject(Provider.NAME).getString("en");
        } catch (JSONException e) {
            return null;
        }
    }

    protected void hideProgressBar() {
        loadingScreen.setVisibility(GONE);
        content.setVisibility(VISIBLE);
    }

    protected void showProgressBar() {
        content.setVisibility(GONE);
        loadingScreen.setVisibility(VISIBLE);
    }

    protected void setProgressbarText(String progressbarText) {
        this.progressbarText.setText(progressbarText);
    }

    protected void setProgressbarText(@StringRes int progressbarText) {
        this.progressbarText.setText(progressbarText);
    }

}
