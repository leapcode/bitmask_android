package se.leap.bitmaskclient.providersetup.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


import butterknife.BindView;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;

public abstract class AbstractProviderDetailActivity extends ConfigWizardBaseActivity {

    final public static String TAG = "providerDetailActivity";

    @BindView(R.id.provider_detail_description)
    AppCompatTextView description;

    @BindView(R.id.provider_detail_options)
    ListView options;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        provider = getIntent().getParcelableExtra(PROVIDER_KEY);
        setContentView(R.layout.a_provider_detail);

        if (provider == null) {
            return;
        }

        setProviderHeaderText(provider.getName());
        description.setText(provider.getDescription());

        // Show only the options allowed by the provider
        ArrayList<String> optionsList = new ArrayList<>();
        if (provider.allowsRegistered()) {
            optionsList.add(getString(R.string.login_to_profile));
            optionsList.add(getString(R.string.create_profile));
            if (provider.allowsAnonymous()) {
                optionsList.add(getString(R.string.use_anonymously_button));
            }
        } else {
            onAnonymouslySelected();
        }

        options.setAdapter(new ArrayAdapter<>(
                this,
                R.layout.v_single_list_item,
                android.R.id.text1,
                optionsList.toArray(new String[optionsList.size()])
        ));
        options.setOnItemClickListener((parent, view, position, id) -> {
            String text = ((AppCompatTextView) view).getText().toString();
            Intent intent;
            if (text.equals(getString(R.string.login_to_profile))) {
                Log.d(TAG, "login selected");
                intent = new Intent(getApplicationContext(), LoginActivity.class);
            } else if (text.equals(getString(R.string.create_profile))) {
                Log.d(TAG, "signup selected");
                intent = new Intent(getApplicationContext(), SignupActivity.class);
            } else {
                onAnonymouslySelected();
                return;
            }
            intent.putExtra(PROVIDER_KEY, provider);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(intent, REQUEST_CODE_CONFIGURE_LEAP);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        provider = intent.getParcelableExtra(PROVIDER_KEY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP) {
            if (resultCode == RESULT_OK) {
                setResult(resultCode, data);
                finish();
            }
        }
    }

    private void onAnonymouslySelected() {
        Intent intent;
        Log.d(TAG, "use anonymously selected");
        intent = new Intent();
        intent.putExtra(Provider.KEY, provider);
        setResult(RESULT_OK, intent);
        finish();
    }

}
