package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getProviderFormattedString;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INITIAL_ACTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.base.models.Provider;

public class ProviderApiEventSender {

    private final Resources resources;
    private final ProviderApiManagerBase.ProviderApiServiceCallback serviceCallback;

    public ProviderApiEventSender(Resources resources, ProviderApiManagerBase.ProviderApiServiceCallback callback) {
        this.resources = resources;
        this.serviceCallback = callback;
    }

    /**
     * Interprets the error message as a JSON object and extract the "errors" keyword pair.
     * If the error message is not a JSON object, then it is returned untouched.
     *
     * @param stringJsonErrorMessage
     * @return final error message
     */
    protected String pickErrorMessage(String stringJsonErrorMessage) {
        String errorMessage = "";
        try {
            JSONObject jsonErrorMessage = new JSONObject(stringJsonErrorMessage);
            errorMessage = jsonErrorMessage.getString(ERRORS);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            errorMessage = stringJsonErrorMessage;
        } catch (NullPointerException e) {
            //do nothing
        }

        return errorMessage;
    }

    protected Bundle setErrorResult(Bundle result, String stringJsonErrorMessage) {
        String reasonToFail = pickErrorMessage(stringJsonErrorMessage);
        VpnStatus.logWarning("[API] error: " + reasonToFail);
        result.putString(ERRORS, reasonToFail);
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }

    Bundle setErrorResultAction(Bundle result, String initialAction) {
        JSONObject errorJson = new JSONObject();
        addErrorMessageToJson(errorJson, null, null, initialAction);
        VpnStatus.logWarning("[API] error: " + initialAction + " failed.");
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }

    Bundle setErrorResult(Bundle result, int errorMessageId, String errorId) {
        return setErrorResult(result, errorMessageId, errorId, null);
    }

    Bundle setErrorResult(Bundle result, int errorMessageId, String errorId, String initialAction) {
        JSONObject errorJson = new JSONObject();
        String errorMessage = getProviderFormattedString(resources, errorMessageId);
        addErrorMessageToJson(errorJson, errorMessage, errorId, initialAction);
        VpnStatus.logWarning("[API] error: " + errorMessage);
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }


    private void addErrorMessageToJson(JSONObject jsonObject, String errorMessage) {
        try {
            jsonObject.put(ERRORS, errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addErrorMessageToJson(JSONObject jsonObject, String errorMessage, String errorId, String initialAction) {
        try {
            jsonObject.putOpt(ERRORS, errorMessage);
            jsonObject.putOpt(ERRORID, errorId);
            jsonObject.putOpt(INITIAL_ACTION, initialAction);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void sendToReceiverOrBroadcast(ResultReceiver receiver, int resultCode, Bundle resultData, Provider provider) {
        if (resultData == null || resultData == Bundle.EMPTY) {
            resultData = new Bundle();
        }
        resultData.putParcelable(PROVIDER_KEY, provider);
        if (receiver != null) {
            receiver.send(resultCode, resultData);
        } else {
            broadcastEvent(resultCode, resultData);
        }
        handleEventSummaryErrorLog(resultCode);
    }

    private void broadcastEvent(int resultCode , Bundle resultData) {
        Intent intentUpdate = new Intent(BROADCAST_PROVIDER_API_EVENT);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(BROADCAST_RESULT_CODE, resultCode);
        intentUpdate.putExtra(BROADCAST_RESULT_KEY, resultData);
        serviceCallback.broadcastEvent(intentUpdate);
    }

    private void handleEventSummaryErrorLog(int resultCode) {
        String event = null;
        switch (resultCode) {
            case INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                event = "download of vpn certificate.";
                break;
            case PROVIDER_NOK:
                event = "setup or update provider details.";
                break;
            case INCORRECTLY_DOWNLOADED_EIP_SERVICE:
                event = "update eip-service.json";
                break;
            case INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                event = "update invalid vpn certificate.";
                break;
            case INCORRECTLY_DOWNLOADED_GEOIP_JSON:
                event = "download menshen service json.";
                break;
            case TOR_TIMEOUT:
            case TOR_EXCEPTION:
                event = "start tor for censorship circumvention";
                break;
            default:
                break;
        }
        if (event != null) {
            VpnStatus.logWarning("[API] failed provider API event: " + event);
        }
    }

    String formatErrorMessage(final int errorStringId) {
        return formatErrorMessage(getProviderFormattedString(resources, errorStringId));
    }

    private String formatErrorMessage(String errorMessage) {
        return "{ \"" + ERRORS + "\" : \"" + errorMessage + "\" }";
    }

    private JSONObject getErrorMessageAsJson(final int toastStringId) {
        try {
            return new JSONObject(formatErrorMessage(toastStringId));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

}
