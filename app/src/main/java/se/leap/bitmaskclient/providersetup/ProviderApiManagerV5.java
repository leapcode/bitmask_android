package se.leap.bitmaskclient.providersetup;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.vpn_certificate_is_invalid;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;
import static se.leap.bitmaskclient.R.string.warning_expired_provider_cert;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.COUNTRYCODE;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_INVALID_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_V5_BRIDGES;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_V5_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_V5_GATEWAYS;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import de.blinkt.openvpn.core.VpnStatus;
import mobile.BitmaskMobile;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.CredentialsParser;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ProviderApiManagerV5 extends ProviderApiManagerBase implements IProviderApiManager {

    private static final String TAG = ProviderApiManagerV5.class.getSimpleName();

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

        try {
            configureBaseCountryCode(bm, parameters);
        } catch (Exception e) {
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        try {
            String serviceJson = bm.getService();
            provider.setService(serviceJson);
        } catch (Exception e) {
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        if (PreferenceHelper.getUseBridges()) {
            try {
                String bridgesJson = bm.getAllBridges("", "", "", "");
                provider.setBridges(bridgesJson);
            } catch (Exception e) {
                // TODO: send failed to fetch bridges event
            }
        } else {
            try {
                String gatewaysJson = bm.getAllGateways("", "", "");
                provider.setGateways(gatewaysJson);
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

        //provider certificate invalid
        if (currentDownload.containsKey(ERRORS)) {
            currentDownload.putParcelable(PROVIDER_KEY, provider);
            return currentDownload;
        }

        BitmaskMobile bm;
        try {
            bm = new BitmaskMobile(provider.getMainUrl(), new PreferenceHelper.SharedPreferenceStore());
            bm.setDebug(BuildConfig.DEBUG);
            if (TorStatusObservable.isRunning() && TorStatusObservable.getSocksProxyPort() != -1) {
                bm.setSocksProxy(SOCKS_PROXY_SCHEME + PROXY_HOST + ":" + TorStatusObservable.getSocksProxyPort());
            }
            if (provider.hasIntroducer()) {
                bm.setIntroducer(provider.getIntroducer().toUrl());
            }
        } catch (Exception e) {
            // TODO: improve error message
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        try {
            configureBaseCountryCode(bm, parameters);
        } catch (Exception e) {
            return eventSender.setErrorResult(currentDownload, R.string.config_error_found, null);
        }

        try {
            String serviceJson = bm.getService();
            Log.d(TAG, "service Json reponse: " + serviceJson);
            provider.setService(serviceJson);
            ProviderSetupObservable.updateProgress(DOWNLOADED_V5_SERVICE_JSON);
        } catch (Exception e) {
            Log.w(TAG, "failed to fetch service.json: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        try {
            // TODO: check if provider supports this API endpoint?
            String gatewaysJson = bm.getAllGateways("", "", "");
            Log.d(TAG, "gateways Json reponse: " + gatewaysJson);
            provider.setGateways(gatewaysJson);
            ProviderSetupObservable.updateProgress(DOWNLOADED_V5_GATEWAYS);
        } catch (Exception e) {
            Log.w(TAG, "failed to fetch gateways: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        try {
            // TODO: check if provider supports this API endpoint?
            String bridgesJson = bm.getAllBridges("", "", "", "");
            Log.d(TAG, "bridges Json reponse: " + bridgesJson);
            provider.setBridges(bridgesJson);
            ProviderSetupObservable.updateProgress(DOWNLOADED_V5_BRIDGES);
        } catch (Exception e) {
            Log.w(TAG, "failed to fetch bridges: " + e.getMessage());
            e.printStackTrace();
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        try {
            String cert = bm.getOpenVPNCert();
            currentDownload = loadCredentials(provider, cert);
            currentDownload = validateCertificateForProvider(currentDownload, provider);
            ProviderSetupObservable.updateProgress(DOWNLOADED_VPN_CERTIFICATE);
        } catch (Exception e) {
            return eventSender.setErrorResult(currentDownload, R.string.error_json_exception_user_message, null);
        }

        return currentDownload;
    }

    private Bundle loadCredentials(Provider provider, String credentials) {
        Bundle result = new Bundle();

        try {
            CredentialsParser.parseXml(credentials, provider);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            return eventSender.setErrorResult(result, vpn_certificate_is_invalid, null);
        }

        result.putBoolean(BROADCAST_RESULT_KEY, true);
        return result;
    }

    @Nullable
    private void configureBaseCountryCode(BitmaskMobile bm, Bundle parameters) throws Exception {
        String cc = parameters.getString(COUNTRYCODE, null);
        if (cc == null &&
                EipStatus.getInstance().isDisconnected() &&
                TorStatusObservable.getStatus() == OFF) {
            try {
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
