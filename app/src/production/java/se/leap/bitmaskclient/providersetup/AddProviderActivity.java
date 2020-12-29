package se.leap.bitmaskclient.providersetup;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.providersetup.activities.AddProviderBaseActivity;

public class AddProviderActivity extends AddProviderBaseActivity {

    final public static String TAG = "AddProviderActivity";

    @BindView(R.id.button_save)
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_add_provider);
        init();

    }



    @Override
    public void setupSaveButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveProvider();
            }
        });
    }
}
