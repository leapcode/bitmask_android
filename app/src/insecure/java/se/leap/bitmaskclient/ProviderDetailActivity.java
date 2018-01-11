package se.leap.bitmaskclient;

import android.content.SharedPreferences;

public class ProviderDetailActivity extends AbstractProviderDetailActivity {

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Provider.KEY).remove(ProviderListContent.ProviderItem.DANGER_ON).remove(Constants.PROVIDER_ALLOW_ANONYMOUS).remove(Constants.PROVIDER_KEY).apply();
        super.onBackPressed();
    }

}
