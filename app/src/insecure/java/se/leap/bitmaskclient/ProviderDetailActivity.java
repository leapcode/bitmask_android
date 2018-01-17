package se.leap.bitmaskclient;

import android.content.SharedPreferences;

import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;

public class ProviderDetailActivity extends AbstractProviderDetailActivity {

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Provider.KEY).remove(ProviderListContent.ProviderItem.DANGER_ON).remove(PROVIDER_ALLOW_ANONYMOUS).remove(PROVIDER_KEY).apply();
        super.onBackPressed();
    }

}
