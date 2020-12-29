package se.leap.bitmaskclient.providersetup;

import android.content.SharedPreferences;

import se.leap.bitmaskclient.base.models.Constants;
import se.leap.bitmaskclient.providersetup.activities.AbstractProviderDetailActivity;

public class ProviderDetailActivity extends AbstractProviderDetailActivity {

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Constants.DANGER_ON).apply();
        super.onBackPressed();
    }

}
