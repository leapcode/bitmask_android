package se.leap.bitmaskclient.providersetup.helpers;

import androidx.fragment.app.Fragment;

import se.leap.bitmaskclient.providersetup.fragments.helpers.AbstractQrScannerHelper;

public class QrScannerHelper extends AbstractQrScannerHelper {

    public QrScannerHelper(Fragment fragment, ScanResultCallback callback) {
        super(fragment, callback);
    }

    @Override
    public void startScan() {
    }
}
