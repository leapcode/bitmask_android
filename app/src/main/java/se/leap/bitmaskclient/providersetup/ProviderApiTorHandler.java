package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.base.utils.ConfigHelper.getTorTimeout;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.ON;

import org.jetbrains.annotations.Blocking;

import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ProviderApiTorHandler {

    ProviderApiManagerBase.ProviderApiServiceCallback serviceCallback;
    public ProviderApiTorHandler(ProviderApiManagerBase.ProviderApiServiceCallback callback) {
        this.serviceCallback = callback;
    }

    @Blocking
    public boolean startTorProxy() throws InterruptedException, IllegalStateException, TimeoutException {
        if (EipStatus.getInstance().isDisconnected() &&
                PreferenceHelper.getUseSnowflake() &&
                serviceCallback.startTorService()) {
            waitForTorCircuits();
            if (TorStatusObservable.isCancelled()) {
                throw new InterruptedException("Cancelled Tor setup.");
            }
            int port = serviceCallback.getTorHttpTunnelPort();
            TorStatusObservable.setProxyPort(port);
            int socksPort = serviceCallback.getTorSocksProxyPort();
            TorStatusObservable.setSocksProxyPort(socksPort);
            return port != -1 && socksPort != -1;
        }
        return false;
    }

    public void stopTorProxy() {
        serviceCallback.stopTorService();
    }

    private void waitForTorCircuits() throws InterruptedException, TimeoutException {
        if (TorStatusObservable.getStatus() == ON) {
            return;
        }
        TorStatusObservable.waitUntil(this::isTorOnOrCancelled, getTorTimeout());
    }

    private boolean isTorOnOrCancelled() {
        return TorStatusObservable.getStatus() == ON || TorStatusObservable.isCancelled();
    }

}
