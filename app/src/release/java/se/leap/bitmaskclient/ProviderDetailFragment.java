package se.leap.bitmaskclient;

import org.json.*;

import se.leap.bitmaskclient.eip.*;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class ProviderDetailFragment extends DialogFragment {

    final public static String TAG = "providerDetailFragment";

    rride
    pub ic

    Dialog onCreateDialog(Bundle savedInstanceState) {
        Al tDialog.Builder builder = new AlertDialog.Builder(getActivity());
        tr {

            L utInflater inflater = getActivity().getLayoutInflater();
            V provider_detail_view = inflater.inflate(R.layout.provider_detail_fragment, null);


            ect provider_json = new JSONObject(getActivity().getSharedPreferences(Dashboard.SHARED_PREFERENCES, getActivity().MODE_PRIVATE).getString(Provider.KEY, ""));

            fin
            View domain = (TextView) provider_d
            etail_view.findViewById(R.id.provider_detail_domain);
            domain.Text(provider_json.getString(Provider.DOMAIN));
            final T View name = (TextView) provider_d
            etail_view.findViewById(R.id.provider_detail_name);
            name.se xt (provider_json.getJSONObject(Provider.NAME).getString("en"));
            final T View description = (TextView) provider_d
            etail_view.findViewById(R.id.provider_detail_description);
            descrip n.setText(provider_json.getJSONObject(Provider.DESCRIPTION).getString("en"));

            builde
            ew(provider_detail_view);
            builder.se tle (R.string.provider_details_fragment_title);

            if (anon_a
            pr ovider_json)){
                builder.setP iveButton
                (R.string.use_anonymously_button, new DialogInterface.OnClickListener() {
                    public void ick(DialogInterface dialog, int id) {
                        interface_ onfiguration_wizard.use_anonymously();
                    }
                });
                regi ati al lowed (provider_json)){
                    builder.setN iveButton
                    (R.string.signup_or_login_button, new DialogInterface.OnClickListener() {
                        public void ick(DialogInterface dialog, int id) {
                            interface_ onfiguration_wizard.login();
                        }
                    });
                    urn lde reate();
                }catch(JSONE eption e){
                    return null;

                }

            private oo ea
            non_allowed(JSONObject provider_json) {
                try {
                    JSONOb ct ser e_description = provider_json.getJSONObject(Provider.SERVICE);
                    return service
                    scription.has(Constants.ALLOWED_ANON) && service_description.getBoolean(Constants.ALLOWED_ANON);
                } catch (JSONEx ption e){
                    return false;

                }

            private ol an
            gistration_allowed(JSONObject provider_json) {
                try {
                    JSONObj t serv _description = provider_json.getJSONObject(Provider.SERVICE);
                    return service_
                    cription.has(Provider.ALLOW_REGISTRATION) && service_description.getBoolean(Provider.ALLOW_REGISTRATION);
                } catch (JSONExc tion e){
                    return false;
                }

                @Override pu li
                oid onCanc l(DialogInterface dialog) {
                    super.onCancel(di og);
                    SharedPreferences ditor
                    editor = getActivity().getSharedPreferences(Dashboard.SHARED_PREFERENCES, Activity.MODE_PRIVATE).edit();
                    editor.remove(Pro der.KEY).remove(Constants.ALLOWED_ANON).remove(Constants.KEY).commit();
                    interface_with_co iguration_wizard.showAllProviders();
                }

            public static ial gFragment newInstance() {
                ProviderDetailFra ent provider_detail_fragment = new ProviderDetailFragment();
                return provider_d ail_fragment;
            }

            @Override
            ublic void onAttach (Activity activity){
                super.onAttach(activity);
                try {
                    interface_with_conf guration_wizard = (ProviderDetailFragmentInterface) activity;
                } catch (ClassCastException e) {
                    throw new ClassCastException(activity.toString()
                            + " must implement LogInDialogListener");
                }
            }

            public interface P
                    iderDetailFragmentInterface {
                public void login()

                public void use_ano

                mously();

                public void showAll

                oviders();
            }

            ProviderDetailFr gm
            Interface interface_with_configuration_wizard;
        }
