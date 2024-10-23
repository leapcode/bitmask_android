package se.leap.bitmaskclient.providersetup.activities.scanner;

import static androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM;
import static androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Introducer;
import se.leap.bitmaskclient.base.utils.ViewHelper;
import se.leap.bitmaskclient.base.views.ActionBarTitle;
import se.leap.bitmaskclient.databinding.AScannerBinding;

public class ScannerActivity extends AppCompatActivity {
    public static final String INVITE_CODE = "invite_code";
    private static final String TAG = ScannerActivity.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final int REQUEST_CODE_PERMISSIONS = 1;

    private AScannerBinding binding;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;

    public static Intent newIntent(Context context) {
        return new Intent(context, ScannerActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupActionBar();
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    private void setupActionBar() {
        setSupportActionBar(binding.toolbar);
        final ActionBar actionBar = getSupportActionBar();
        Context context = actionBar.getThemedContext();
        actionBar.setDisplayOptions(DISPLAY_SHOW_CUSTOM);

        ActionBarTitle actionBarTitle = new ActionBarTitle(context);
        actionBarTitle.setTitleCaps(BuildConfig.actionbar_capitalize_title);
        actionBarTitle.setTitle(getString(R.string.scan_qr_code));

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_back, getTheme());
        actionBar.setHomeAsUpIndicator(upArrow);

        actionBar.setDisplayHomeAsUpEnabled(true);
        ViewHelper.setActivityBarColor(this, R.color.bg_setup_status_bar, R.color.bg_setup_action_bar, R.color.colorActionBarTitleFont);
        @ColorInt int titleColor = ContextCompat.getColor(context, R.color.colorActionBarTitleFont);
        actionBarTitle.setTitleTextColor(titleColor);

        actionBarTitle.setCentered(BuildConfig.actionbar_center_title);
        actionBarTitle.setSingleBoldTitle();
        if (BuildConfig.actionbar_center_title) {
            ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            actionBar.setCustomView(actionBarTitle, params);
        } else {
            actionBar.setCustomView(actionBarTitle);
        }
    }

    private void startCamera() {
        var cameraController = new LifecycleCameraController(getBaseContext());
        var previewView = binding.previewView;

        var options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        barcodeScanner = BarcodeScanning.getClient(options);

        var mlKitAnalyzer = new MlKitAnalyzer(
                Collections.singletonList(barcodeScanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this),
                result -> handleQrResult(result, previewView)
        );

        cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this), mlKitAnalyzer);

        cameraController.bindToLifecycle(this);
        previewView.setController(cameraController);
    }

    private void handleQrResult(MlKitAnalyzer.Result result, PreviewView previewView) {
        var barcodeResults = result.getValue(barcodeScanner);
        if ((barcodeResults == null) || (barcodeResults.isEmpty()) || (barcodeResults.get(0) == null)) {
            previewView.getOverlay().clear();
            previewView.setOnTouchListener((v, event) -> false); //no-op
            return;
        }
        try {
            Introducer introducer = Introducer.fromUrl(barcodeResults.get(0).getRawValue());
            if (introducer.validate()) {
                setResult(RESULT_OK, new Intent().putExtra(INVITE_CODE, introducer));
            } else {
                Toast.makeText(this, R.string.invalid_code, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Introducer error: " + e.getMessage());
            Toast.makeText(this, R.string.invalid_code, Toast.LENGTH_SHORT).show();
        }
        previewView.setOnTouchListener((v, event) -> false); //no-op
        previewView.getOverlay().clear();

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
