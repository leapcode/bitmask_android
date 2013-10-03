package se.leap.bitmaskclient;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ProviderDetailFragment extends DialogFragment {

    final public static String TAG = "providerDetailFragment";
    
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		try {

			LayoutInflater inflater = getActivity().getLayoutInflater();
			View provider_detail_view = inflater.inflate(R.layout.provider_detail_fragment, null);
			
			JSONObject provider_json = ConfigHelper.getJsonFromSharedPref(Provider.KEY);
			
			final TextView domain = (TextView)provider_detail_view.findViewById(R.id.provider_detail_domain);
			domain.setText(provider_json.getString(Provider.DOMAIN));
			final TextView name = (TextView)provider_detail_view.findViewById(R.id.provider_detail_name);
			name.setText(provider_json.getJSONObject(Provider.NAME).getString("en"));
			final TextView description = (TextView)provider_detail_view.findViewById(R.id.provider_detail_description);
			description.setText(provider_json.getJSONObject(Provider.DESCRIPTION).getString("en"));
			
			builder.setView(provider_detail_view);
			builder.setTitle(R.string.provider_details_fragment_title);
			
			if(anon_allowed(provider_json)) {
				builder.setPositiveButton(R.string.use_anonymously_button, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						interface_with_configuration_wizard.use_anonymously();
					}
				});
			}

			if(registration_allowed(provider_json)) {
				builder.setNegativeButton(R.string.login_button, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						interface_with_configuration_wizard.login();
					}
				});
			}
			
			return builder.create();
		} catch (JSONException e) {
			return null;
		}
	}
	
	private boolean anon_allowed(JSONObject provider_json) {
		try {
			JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
			return service_description.has(EIP.ALLOWED_ANON) && service_description.getBoolean(EIP.ALLOWED_ANON);
		} catch (JSONException e) {
			return false;
		}
	}
	
	private boolean registration_allowed(JSONObject provider_json) {
		try {
			JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
			return service_description.has(Provider.ALLOW_REGISTRATION) && service_description.getBoolean(Provider.ALLOW_REGISTRATION);
		} catch (JSONException e) {
			return false;
		}
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		ConfigHelper.removeFromSharedPref(Provider.KEY);
		ConfigHelper.removeFromSharedPref(ProviderItem.DANGER_ON);
		ConfigHelper.removeFromSharedPref(EIP.ALLOWED_ANON);
		ConfigHelper.removeFromSharedPref(EIP.KEY);
		interface_with_configuration_wizard.showAllProviders();
	}

	public static DialogFragment newInstance() {
		ProviderDetailFragment provider_detail_fragment = new ProviderDetailFragment();
		return provider_detail_fragment;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	interface_with_configuration_wizard = (ProviderDetailFragmentInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LogInDialogListener");
        }
    }
	
	public interface ProviderDetailFragmentInterface {
		public void login();
		public void use_anonymously();
		public void showAllProviders();
	}
	
	ProviderDetailFragmentInterface interface_with_configuration_wizard;
}
