package se.leap.bitmaskclient.motd;

import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import de.blinkt.openvpn.core.VpnStatus;
import motd.IMessages;
import motd.IMotd;
import motd.Motd;
import se.leap.bitmaskclient.base.models.Provider;

public class MotdClient {
    IMotd motd;

    public MotdClient(Provider provider) {
        motd = Motd.newMotd(provider.getMotdUrl().toString(), provider.getName(), "android");
    }

    @WorkerThread
    public IMessages fetch() {
        if (!VpnStatus.isVPNActive()) {
            VpnStatus.logError("Tried to fetch Message of the Day while VPN was off.");
            return null;
        }

        return Motd.newMessages(motd.fetchLatestAsJson());
    }

    @WorkerThread
    public JSONObject fetchJson() {
        if (!VpnStatus.isVPNActive()) {
            VpnStatus.logError("Tried to fetch Message of the Day while VPN was off.");
            return null;
        }

        try {
            return new JSONObject(motd.fetchLatestAsJson());
        } catch (NullPointerException | JSONException e) {
            e.printStackTrace();
            return null;
        }

    }

}
