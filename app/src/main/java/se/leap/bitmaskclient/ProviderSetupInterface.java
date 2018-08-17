package se.leap.bitmaskclient;

import android.os.Bundle;

/**
 * Created by cyberta on 17.08.18.
 */

public interface ProviderSetupInterface {
    enum ProviderConfigState {
        PROVIDER_NOT_SET,
        SETTING_UP_PROVIDER,
        SHOWING_PROVIDER_DETAILS,
        PENDING_SHOW_FAILED_DIALOG,
        SHOW_FAILED_DIALOG,
    }

    void handleProviderSetUp(Provider provider);
    void handleProviderSetupFailed(Bundle resultData);
    void handleCorrectlyDownloadedCertificate(Provider provider);
    void handleIncorrectlyDownloadedCertificate();
    Provider getProvider();
    ProviderConfigState getConfigState();
}
