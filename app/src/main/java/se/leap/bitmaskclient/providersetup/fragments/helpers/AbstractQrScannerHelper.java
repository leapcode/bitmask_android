package se.leap.bitmaskclient.providersetup.fragments.helpers;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import se.leap.bitmaskclient.base.models.Introducer;

public abstract class AbstractQrScannerHelper {
    public interface ScanResultCallback {
        void onScanResult(Introducer introducer);
        void onScanError(JSONObject error);
    }

    public AbstractQrScannerHelper(Fragment fragment, ScanResultCallback callback) {
    }

    public abstract void startScan();
}
