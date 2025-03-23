package se.leap.bitmaskclient.providersetup.helpers;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import se.leap.bitmaskclient.base.models.Introducer;
import se.leap.bitmaskclient.providersetup.fragments.helpers.AbstractQrScannerHelper;

public class QrScannerHelper extends AbstractQrScannerHelper {

    private final ActivityResultLauncher<ScanOptions> scannerActivityResultLauncher;

    public QrScannerHelper(Fragment fragment, ScanResultCallback callback) {
        super(fragment, callback);
        this.scannerActivityResultLauncher = fragment.registerForActivityResult(new ScanContract(), result -> {
            if(result.getContents() != null) {
                try {
                    Introducer introducer = Introducer.fromUrl(result.getContents());
                    callback.onScanResult(introducer);
                } catch (Exception e) {
                    e.printStackTrace();
                    //binding.editCustomProvider.setText(result.getContents());
                }
            }
        });
    }

    @Override
    public void startScan() {
        ScanOptions options = new ScanOptions();
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);
        scannerActivityResultLauncher.launch(options);
    }
}
