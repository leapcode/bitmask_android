package se.leap.bitmaskclient;

import android.content.SharedPreferences;

public class ProviderDetailActivity extends AbstractProviderDetailActivity {

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(ProviderListContent.ProviderItem.DANGER_ON).apply();
        super.onBackPressed();
    }

}
