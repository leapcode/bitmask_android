package se.leap.bitmaskclient.providersetup.helpers;

import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERROR_EXTRA;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.SETUP_ERRORS.ERROR_QR_CODE_SCANNING;

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Introducer;
import se.leap.bitmaskclient.providersetup.fragments.helpers.AbstractQrScannerHelper;

public class QrScannerHelper extends AbstractQrScannerHelper {

    private final ActivityResultLauncher<ScanOptions> scannerActivityResultLauncher;

    public QrScannerHelper(Fragment fragment, ScanResultCallback callback) {
        super(fragment, callback);
        this.scannerActivityResultLauncher = fragment.registerForActivityResult(new ScanContract(), result -> {
            if(result.getContents() == null) {
                return;
            }
            try {
                Introducer introducer = Introducer.fromUrl(result.getContents());
                callback.onScanResult(introducer);
            } catch (Introducer.IntroducerException e) {
                e.printStackTrace();
                JSONObject jsonObject = new JSONObject();
                try {
                    Context c = fragment.getContext();
                    jsonObject.put(ERRORS, c.getString(R.string.error_invite_title) + " " +  e.getLocalizedMessage(c) + "\n\n" + c.getString(R.string.error_invite_hint));
                    jsonObject.put(ERRORID, ERROR_QR_CODE_SCANNING.toString());
                    jsonObject.put(ERROR_EXTRA, c.getString(R.string.error_invite, "\n" + result.getContents().trim()));
                    callback.onScanError(jsonObject);
                } catch (JSONException | NullPointerException ex) {
                    ex.printStackTrace();
                }
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
        });
    }

    @Override
    public void startScan() {
        ScanOptions options = new ScanOptions();
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);
        options.setPrompt("");
        scannerActivityResultLauncher.launch(options);
    }
}
