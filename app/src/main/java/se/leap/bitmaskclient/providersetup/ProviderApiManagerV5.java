package se.leap.bitmaskclient.providersetup;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;
import static se.leap.bitmaskclient.R.string.warning_expired_provider_cert;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.COUNTRYCODE;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_INVALID_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_EIP_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_PROVIDER_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.Nullable;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import de.blinkt.openvpn.core.VpnStatus;
import io.swagger.client.JSON;
import io.swagger.client.model.ModelsBridge;
import io.swagger.client.model.ModelsEIPService;
import io.swagger.client.model.ModelsGateway;
import io.swagger.client.model.ModelsProvider;
import mobile.BitmaskMobile;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ProviderApiManagerV5 extends ProviderApiManagerBase implements IProviderApiManager {

    private static final String TAG = ProviderApiManagerV5.class.getSimpleName();
    private static final String PROXY_HOST = "127.0.0.1";
    private static final String SOCKS_PROXY_SCHEME = "socks5://";

    ProviderApiManagerV5(Resources resources, ProviderApiServiceCallback callback) {
        super(resources, callback);
    }

    @Override
    public void handleAction(String action, Provider provider, Bundle parameters, ResultReceiver receiver) {
        Bundle result = new Bundle();
        switch (action) {
            case SET_UP_PROVIDER:
                result = setupProvider(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_OK, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result, provider);
                }
                break;

            case DOWNLOAD_SERVICE_JSON:
                result = updateServiceInfos(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    serviceCallback.saveProvider(provider);
                    eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_EIP_SERVICE, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_EIP_SERVICE, result, provider);
                }
                break;

            case DOWNLOAD_VPN_CERTIFICATE:
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    serviceCallback.saveProvider(provider);
                    ProviderSetupObservable.updateProgress(DOWNLOADED_VPN_CERTIFICATE);
                    eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                }
                break;
            case QUIETLY_UPDATE_VPN_CERTIFICATE:
            case UPDATE_INVALID_VPN_CERTIFICATE:
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    Log.d(TAG, "successfully downloaded VPN certificate");
                    provider.setShouldUpdateVpnCertificate(false);
                    PreferenceHelper.storeProviderInPreferences(provider);
                    ProviderObservable.getInstance().updateProvider(provider);
                }
                break;
        }

    }

    private Bundle updateServiceInfos(Provider provider, Bundle parameters) {
        Bundle currentDownload = new Bundle();

        BitmaskMobile bm;
        try {
            bm = new BitmaskMobile(provider.getMainUrl(), new PreferenceHelper.SharedPreferenceStore());
        } catch (IllegalStateException e) {
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        configureBaseCountryCode(bm, parameters);

        try {
            String serviceJson = bm.getService();
            ModelsEIPService service = JSON.createGson().create().fromJson(serviceJson, ModelsEIPService.class);
            provider.setService(service);
        } catch (Exception e) {
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        if (PreferenceHelper.getUseBridges()) {
            try {
                String bridgesJson = bm.getAllBridges("", "", "", "");
                if (bridgesJson.isEmpty())  {
                    //TODO send no bridges error event
                }
                ModelsBridge[] bridges = JSON.createGson().create().fromJson(bridgesJson, ModelsBridge[].class);
                provider.setBridges(bridges);
            } catch (Exception e) {
                // TODO: send failed to fetch bridges event
            }
        } else {
           try {
               String gatewaysJson = bm.getAllGateways("", "", "");
               if (gatewaysJson.isEmpty())  {
                   //TODO send no bridges error event
               }
               ModelsGateway[] gateways = JSON.createGson().create().fromJson(gatewaysJson, ModelsGateway[].class);

               provider.setGateways(gateways);
            } catch (Exception e) {
                // TODO: send
                return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);

            }
        }

        return currentDownload;

    }

    protected Bundle setupProvider(Provider provider, Bundle parameters) {
        Bundle currentDownload = new Bundle();

        if (isEmpty(provider.getMainUrl()) || provider.getMainUrl().isEmpty()) {
            currentDownload.putBoolean(BROADCAST_RESULT_KEY, false);
            eventSender.setErrorResult(currentDownload, malformed_url, null);
            VpnStatus.logWarning("[API] MainURL String is not set. Cannot setup provider.");
            return currentDownload;
        }

        getPersistedProviderUpdates(provider);
        currentDownload = validateProviderDetails(provider);

        //provider certificate invalid
        if (currentDownload.containsKey(ERRORS)) {
            currentDownload.putParcelable(PROVIDER_KEY, provider);
            return currentDownload;
        }

        //no provider json or certificate available
        if (currentDownload.containsKey(BROADCAST_RESULT_KEY) && !currentDownload.getBoolean(BROADCAST_RESULT_KEY)) {
            resetProviderDetails(provider);
        }

        if (currentDownload.containsKey(PROVIDER_KEY)) {
            provider = currentDownload.getParcelable(PROVIDER_KEY);
        }
        BitmaskMobile bm;
        try {
            bm = new BitmaskMobile(provider.getMainUrl(), new PreferenceHelper.SharedPreferenceStore());
            if (TorStatusObservable.isRunning() && TorStatusObservable.getSocksProxyPort() != -1) {
                bm.setSocksProxy(SOCKS_PROXY_SCHEME + PROXY_HOST + ":" + TorStatusObservable.getSocksProxyPort());
            }
        } catch (IllegalStateException e) {
            // TODO: improve error message
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        configureBaseCountryCode(bm, parameters);

        try {
           String providerJson = bm.getProvider();
           Log.d(TAG, "provider Json reponse: " + providerJson);
           ModelsProvider p = JSON.createGson().create().fromJson(providerJson, ModelsProvider.class);
           provider.setModelsProvider(p);
           ProviderSetupObservable.updateProgress(DOWNLOADED_PROVIDER_JSON);
        } catch (Exception e) {
            Log.w(TAG, "failed fo fetch provider.json: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }
        try {
            String serviceJson = bm.getService();
            Log.d(TAG, "service Json reponse: " + serviceJson);
            ModelsEIPService service = JSON.createGson().create().fromJson(serviceJson, ModelsEIPService.class);
            provider.setService(service);
            ProviderSetupObservable.updateProgress(DOWNLOADED_EIP_SERVICE_JSON);
        } catch (Exception e) {
            Log.w(TAG, "failed to fetch service.json: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        try {
            // TODO: check if provider supports this API endpoint?
            String gatewaysJson = bm.getAllGateways("", "", "");
            Log.d(TAG, "gateways Json reponse: " + gatewaysJson);
            ModelsGateway[] gateways = JSON.createGson().create().fromJson(gatewaysJson, ModelsGateway[].class);
            provider.setGateways(gateways);
        } catch (Exception e) {
            Log.w(TAG, "failed to fetch gateways: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        try {
            // TODO: check if provider supports this API endpoint?
            String bridgesJson = bm.getAllBridges("", "", "", "");
            Log.d(TAG, "bridges Json reponse: " + bridgesJson);
            ModelsBridge[] bridges = JSON.createGson().create().fromJson(bridgesJson, ModelsBridge[].class);
            provider.setBridges(bridges);
        } catch (Exception e) {
            Log.w(TAG, "failed to fetch bridges: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        try {
            String cert = bm.getOpenVPNCert();
            currentDownload = loadCertificate(provider, cert);
        } catch (Exception e) {
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        return currentDownload;
    }

    @Nullable
    private void configureBaseCountryCode(BitmaskMobile bm, Bundle parameters) {
        String cc = parameters.getString(COUNTRYCODE, null);
        if (cc == null &&
                !EipStatus.getInstance().isDisconnected() &&
                TorStatusObservable.getStatus() == OFF) {
            try {
                // FIXME: doGeolocationLookup currently sets the country code implicitly, change that in bitmask-core
                cc = bm.getGeolocation();
            } catch (Exception e) {
                // print exception and ignore
                e.printStackTrace();
                cc = "";
            }
        }
        bm.setCountryCode(cc);
    }

    Bundle validateProviderDetails(Provider provider) {
        Bundle result = new Bundle();
        result.putBoolean(BROADCAST_RESULT_KEY, false);

        if (!provider.hasDefinition()) {
            return result;
        }

        result = validateCertificateForProvider(result, provider);

        //invalid certificate or no certificate or unable to connect due other connectivity issues
        if (result.containsKey(ERRORS) || (result.containsKey(BROADCAST_RESULT_KEY) && !result.getBoolean(BROADCAST_RESULT_KEY)) ) {
            return result;
        }

        result.putBoolean(BROADCAST_RESULT_KEY, true);

        return result;
    }

    protected Bundle validateCertificateForProvider(Bundle result, Provider provider) {
        String caCert = provider.getCaCert();

        if (ConfigHelper.checkErroneousDownload(caCert)) {
            VpnStatus.logWarning("[API] No provider cert.");
            return result;
        }

        ArrayList<X509Certificate> certificates = ConfigHelper.parseX509CertificatesFromString(caCert);
        if (certificates == null) {
            return eventSender.setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }

        ArrayList<X509Certificate> validCertificates = new ArrayList<>();
        int invalidCertificates = 0;
        for (X509Certificate certificate : certificates) {
            try {
                certificate.checkValidity();
                validCertificates.add(certificate);
            } catch (CertificateNotYetValidException |
                    CertificateExpiredException e) {
                e.printStackTrace();
                invalidCertificates++;
            }
        }
        if (validCertificates.isEmpty() && invalidCertificates > 0) {
            return eventSender.setErrorResult(result, warning_expired_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }

        provider.setCaCert(ConfigHelper.parseX509CertificatesToString(validCertificates));
        result.putParcelable(PROVIDER_KEY, provider);
        result.putBoolean(BROADCAST_RESULT_KEY, true);
        return result;
    }

    protected Bundle updateVpnCertificate(Provider provider) {
        return null;
    }

}
