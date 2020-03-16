package se.leap.bitmaskclient;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.InjectView;

public class AddProviderActivity extends AddProviderBaseActivity {

    final public static String TAG = "AddProviderActivity";

    @InjectView(R.id.button_save)
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
