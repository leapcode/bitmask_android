package se.leap.bitmaskclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddProviderActivity extends ConfigWizardBaseActivity {

    private EditText editText;
    final public static String TAG = "AddProviderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.a_add_provider);
        editText = findViewById(R.id.textUri);
        if (this.getIntent().getExtras() != null) {
            editText.setText(this.getIntent().getExtras().getString("invalid_url"));
        }
        final Button saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "On Click when save works");
                saveProvider();
            }
        });

        final Button cancelButton = findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                //ToDo: Implement this and set testNewURL in ProviderListBaseActivty false!!
               /* Intent intent= new Intent();
            intent.putExtra("new_url", entered_url);
            setResult(RESULT_NOT_OK, intent);
            finish();*/
            }
        });
        setUpInitialUI();
    }

    private void setUpInitialUI() {
        //ToDo: find out if needed:
        //  setContentView(R.layout.a_provider_list);
        setProviderHeaderText(R.string.add_provider);
        hideProgressBar();
    }


    private void saveProvider() {
        String entered_url = editText.getText().toString().trim();
        if (!entered_url.startsWith("https://")) {
            if (entered_url.startsWith("http://")) {
                entered_url = entered_url.substring("http://".length());
            }
            entered_url = "https://".concat(entered_url);
        }
        Log.d(TAG, "Behind https addition");
        if (validURL(entered_url)) {
            Log.d(TAG, "URL seems fine");
            Intent intent = this.getIntent();
            intent.putExtra("new_url", entered_url);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Log.d(TAG, "Bad URL.");
            editText.setText("");
            Toast.makeText(this.getApplicationContext(), R.string.not_valid_url_entered, Toast.LENGTH_LONG).show();
        }
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
