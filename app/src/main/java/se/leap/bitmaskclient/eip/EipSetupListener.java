package se.leap.bitmaskclient.eip;

import android.content.Intent;

/**
 * Created by cyberta on 05.12.18.
 */
public interface EipSetupListener {
    void handleEipEvent(Intent intent);

    void handleProviderApiEvent(Intent intent);
}
