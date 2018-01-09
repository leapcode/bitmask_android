package se.leap.bitmaskclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ProviderDetailActivity extends Activity {

    final public static String TAG = "providerDetailActivity";
    private SharedPreferences preferences;

    @InjectView(R.id.provider_detail_domain)
    TextView domain;

    @InjectView(R.id.provider_detail_name)
    TextView name;

    @InjectView(R.id.provider_detail_description)
    TextView description;

    @InjectView(R.id.provider_detail_options)
    ListView options;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_detail_fragment);
        ButterKnife.inject(this);

        preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        try {
            JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
            domain.setText(provider_json.getString(Provider.DOMAIN));
            name.setText(provider_json.getJSONObject(Provider.NAME).getString("en"));
            description.setText(provider_json.getJSONObject(Provider.DESCRIPTION).getString("en"));

            setTitle(R.string.provider_details_title);

            ArrayList<String> optionsList = new ArrayList<>();
            optionsList.add(getString(R.string.login_button));
            if (registration_allowed(provider_json)) {
                optionsList.add(getString(R.string.signup_button));
            }
            if (anon_allowed(provider_json)) {
                optionsList.add(getString(R.string.use_anonymously_button));
            }

            options.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    optionsList.toArray(new String[optionsList.size()])
            ));
            options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String text = ((TextView) view).getText().toString();
                    if (text.equals(getString(R.string.login_button))) {
                        Log.d(TAG, "login selected");
                    } else if (text.equals(getString(R.string.signup_button))) {
                        Log.d(TAG, "signup selected");
                    } else if (text.equals(getString(R.string.use_anonymously_button))) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    }
                }
            });
        } catch (JSONException e) {
            // TODO show error and return
        }
    }

    private boolean anon_allowed(JSONObject provider_json) {
        try {
            JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
            return service_description.has(Constants.PROVIDER_ALLOW_ANONYMOUS) && service_description.getBoolean(Constants.PROVIDER_ALLOW_ANONYMOUS);
        } catch (JSONException e) {
            return false;
        }
    }

    private boolean registration_allowed(JSONObject provider_json) {
        try {
            JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
            return service_description.has(Provider.ALLOW_REGISTRATION) && service_description.getBoolean(Provider.ALLOW_REGISTRATION);
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Provider.KEY).remove(ProviderItem.DANGER_ON).remove(Constants.PROVIDER_ALLOW_ANONYMOUS).remove(Constants.PROVIDER_KEY).apply();
        super.onBackPressed();
    }

}
