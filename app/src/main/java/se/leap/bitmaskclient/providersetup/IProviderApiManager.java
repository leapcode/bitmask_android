package se.leap.bitmaskclient.providersetup;

import android.os.Bundle;
import android.os.ResultReceiver;

import se.leap.bitmaskclient.base.models.Provider;

public interface IProviderApiManager {
    void handleAction(String action, Provider provider, Bundle parameters, ResultReceiver receiver);

}
