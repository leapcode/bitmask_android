package se.leap.leapclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Models the provider list shown in the ConfigurationWizard.
 * 
 * @author parmegv
 *
 */
public class ProviderListContent {

    public static List<ProviderItem> ITEMS = new ArrayList<ProviderItem>();

    public static Map<String, ProviderItem> ITEM_MAP = new HashMap<String, ProviderItem>();
    
    /**
     * Adds a new provider item to the end of the items map, and to the items list.
     * @param item
     */
    public static void addItem(ProviderItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(String.valueOf(ITEMS.size()), item);
    }

    /**
     * A provider item.
     */
    public static class ProviderItem {  
    	public boolean custom = false;
    	public String id;
        public String name;
        public String provider_json_url;
		public JSONObject provider_json;
        public String provider_json_filename;
        public String eip_service_json_url;
        public String cert_json_url;
        public boolean danger_on = false;

        /**
         * @param name of the provider
         * @param urls_file_input_stream file input stream linking with the assets url file
         * @param custom if it's a new provider entered by the user or not
         * @param danger_on if the user trusts completely the new provider
         */
        public ProviderItem(String name, InputStream urls_file_input_stream, boolean custom, boolean danger_on) {
        	
        	try {
        		byte[] urls_file_bytes = new byte[urls_file_input_stream.available()];
        		urls_file_input_stream.read(urls_file_bytes);
        		String urls_file_content = new String(urls_file_bytes);
				JSONObject file_contents = new JSONObject(urls_file_content);
				id = name;
				this.name = name;
				provider_json_url = file_contents.getString("json_provider");
				provider_json_filename = file_contents.getString("assets_json_provider");
				eip_service_json_url = file_contents.getString("json_eip_service");
				cert_json_url = file_contents.getString("cert");
				this.custom = custom;
				this.danger_on = danger_on;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        /**
         * @param name of the provider
         * @param provider_json_url used to download provider.json file of the provider
         * @param provider_json already downloaded
         * @param custom if it's a new provider entered by the user or not
         * @param danger_on if the user trusts completely the new provider
         */
        public ProviderItem(String name, String provider_json_url, JSONObject provider_json, boolean custom, boolean danger_on) {
        	
        	try {
				id = name;
				this.name = name;
				this.provider_json_url = provider_json_url;
				this.provider_json = provider_json;
				eip_service_json_url = provider_json.getString("api_uri") + "/" + provider_json.getString("api_version") + "/" + ConfigHelper.EIP_SERVICE_API_PATH;
				cert_json_url = (String) provider_json.get("ca_cert_uri");
				this.custom = custom;
				this.danger_on = danger_on;
				if(custom)
					provider_json_filename = name + "_provider.json".replaceFirst("__", "_");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
