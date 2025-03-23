package se.leap.bitmaskclient.providersetup.fragments.helpers;

import androidx.fragment.app.Fragment;

import se.leap.bitmaskclient.base.models.Introducer;

public abstract class AbstractQrScannerHelper {
    public interface ScanResultCallback {
        void onScanResult(Introducer introducer);
    }

    public AbstractQrScannerHelper(Fragment fragment, ScanResultCallback callback) {
    }

    public abstract void startScan();
}
