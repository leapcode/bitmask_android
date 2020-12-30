package se.leap.bitmaskclient.providersetup;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import butterknife.BindView;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.providersetup.activities.AddProviderBaseActivity;

import static android.widget.RelativeLayout.BELOW;
import static android.widget.RelativeLayout.LEFT_OF;
import static se.leap.bitmaskclient.base.models.Constants.DANGER_ON;

public class AddProviderActivity extends AddProviderBaseActivity {

    final public static String TAG = "AddProviderActivity";

    @BindView(R.id.danger_checkbox)
    CheckBox checkboxDanger;

    @BindView(R.id.button_save)
    Button saveButton;

    @Nullable
    @BindView(R.id.button_container)
    LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_add_provider);
        init();

        checkboxDanger.setVisibility(View.VISIBLE);
        checkboxDanger.setText(R.string.danger_checkbox);
        checkboxDanger.setChecked(preferences.getBoolean(DANGER_ON, false));
    }

    @Override
    public void setupSaveButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                preferences.edit().putBoolean(DANGER_ON, checkboxDanger.isChecked()).apply();
                saveProvider();
            }
        });
    }

    @Override
    protected void showCompactLayout() {
        if (isCompactLayout) {
            return;
        }
        super.showCompactLayout();
        showCompactButtonLayout();
    }

    @Override
    protected void showStandardLayout() {
        if (!isCompactLayout) {
            return;
        }
        super.showStandardLayout();
        showStandardButtonLayout();
    }

    private void showCompactButtonLayout() {
        RelativeLayout.LayoutParams phoneButtonContainerParams = (RelativeLayout.LayoutParams) buttonContainer.getLayoutParams();
        phoneButtonContainerParams.addRule(BELOW, 0);
        buttonContainer.setLayoutParams(phoneButtonContainerParams);

        RelativeLayout.LayoutParams checkBoxParams = (RelativeLayout.LayoutParams) checkboxDanger.getLayoutParams();
        checkBoxParams.addRule(LEFT_OF, R.id.button_container);
        checkboxDanger.setLayoutParams(checkBoxParams);
    }

    private void showStandardButtonLayout() {
        RelativeLayout.LayoutParams phoneButtonContainerParams = (RelativeLayout.LayoutParams) buttonContainer.getLayoutParams();
        phoneButtonContainerParams.addRule(BELOW, R.id.danger_checkbox);
        buttonContainer.setLayoutParams(phoneButtonContainerParams);

        RelativeLayout.LayoutParams checkBoxParams = (RelativeLayout.LayoutParams) checkboxDanger.getLayoutParams();
        checkBoxParams.addRule(LEFT_OF, 0);
        checkboxDanger.setLayoutParams(checkBoxParams);
    }
}
