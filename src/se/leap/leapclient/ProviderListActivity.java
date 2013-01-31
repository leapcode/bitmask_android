package se.leap.leapclient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import se.leap.leapclient.ProviderListContent;
import se.leap.leapclient.ProviderListContent.ProviderItem;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;


/**
 * An activity representing a list of Providers. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link DashboardActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProviderListFragment} and the item details
 * (if present) is a {@link DashboardFragment}.
 * <p>
 * This activity also implements the required
 * {@link ProviderListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ProviderListActivity extends FragmentActivity
        implements ProviderListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    
    private static SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_list);

        preferences = getPreferences(MODE_PRIVATE);
        
        loadPreseededProviders();

        // TODO: If exposing deep links into your app, handle intents here.
    }

    private void loadPreseededProviders() {
        AssetManager asset_manager = getAssets();
        String[] urls_filepaths = null;
		try {
			String url_files_folder = "urls";
			//TODO Put that folder in a better place (also inside the "for")
			urls_filepaths = asset_manager.list(url_files_folder); 
			String provider_name = "";
	        for(String url_filepath : urls_filepaths)
	        {
	        	provider_name = url_filepath.subSequence(0, url_filepath.indexOf(".")).toString();
	        	if(ProviderListContent.ITEMS.isEmpty()) //TODO I have to implement a way of checking if a provider new or is already present in that ITEMS list
	        		ProviderListContent.addItem(new ProviderItem(provider_name, asset_manager.open(url_files_folder + "/" + url_filepath)));
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
     * Callback method from {@link ProviderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            
        	Iterator<ProviderItem> preseeded_providers_iterator = ProviderListContent.ITEMS.iterator();
        	while(preseeded_providers_iterator.hasNext())
        	{
        		ProviderItem current_provider_item = preseeded_providers_iterator.next();
        		if(current_provider_item.id.equalsIgnoreCase(id))
        		{
        			ArrayList<String> downloaded_files = downloadFiles(current_provider_item);
        			try {
						parseToSharedPrefs(downloaded_files);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        	
            Intent dashboardIntent = new Intent(this, Dashboard.class);
            startActivity(dashboardIntent);
        }
    }

	private void parseToSharedPrefs(ArrayList<String> downloaded_files) throws IOException {
		SharedPreferences.Editor sharedPreferencesEditor = preferences.edit();
		
		for(int i = 0; i < downloaded_files.size(); i++)
		{
			String type_and_filename = downloaded_files.get(i);
			String filename = type_and_filename.substring(type_and_filename.indexOf(":")+1, type_and_filename.length());
			
			String file_contents = readFileAsString(filename, null);
			
			if(downloaded_files.get(i).startsWith("provider:"))
				sharedPreferencesEditor.putString("provider", file_contents);
			else if(downloaded_files.get(i).startsWith("provider:"))
				sharedPreferencesEditor.putString("eip", file_contents);
			
			sharedPreferencesEditor.commit();
		}
	}
	
	public static String readFileAsString(String fileName, String charsetName)
			throws java.io.IOException {
		java.io.InputStream is = new java.io.FileInputStream(fileName);
		try {
			final int bufsize = 4096;
			int available = is.available();
			byte data[] = new byte[available < bufsize ? bufsize : available];
			int used = 0;
			while (true) {
				if (data.length - used < bufsize) {
					byte newData[] = new byte[data.length << 1];
					System.arraycopy(data, 0, newData, 0, used);
					data = newData;
				}
				int got = is.read(data, used, data.length - used);
				if (got <= 0)
					break;
				used += got;
			}
			return charsetName != null ? new String(data, 0, used, charsetName)
					: new String(data, 0, used);
		} finally {
			is.close();
		}
	}

	private ArrayList<String> downloadFiles(ProviderItem current_provider_item) {
		ArrayList<String> paths_to_downloaded_files = new ArrayList<String>();
		
		paths_to_downloaded_files.add(downloadProviderJSON("provider_json", current_provider_item.provider_json_url, Environment.DIRECTORY_DOWNLOADS, current_provider_item.name + "_provider.json"));
		paths_to_downloaded_files.add(downloadProviderJSON("eip_service_json", current_provider_item.eip_service_json_url, Environment.DIRECTORY_DOWNLOADS, current_provider_item.name + "_eip-service.json"));
		
		return paths_to_downloaded_files;
	}

	private String downloadProviderJSON(String type, String json_url, String target_directory, String filename) {
		
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(json_url));
		request.setDescription("Downloading JSON file");
		request.setTitle("JSON file");
		// in order for this if to run, you must use the android 3.2 to compile your app
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    request.allowScanningByMediaScanner();
		    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		//request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, current_provider_item.name + "_provider.json");
		request.setDestinationInExternalPublicDir(target_directory, filename);

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
		return type + ":" + target_directory + "/" + filename;
		
	}
}
