package se.leap.bitmaskclient;

import android.content.res.AssetManager;

import com.pedrogomez.renderers.AdapteeCollection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderManager implements AdapteeCollection<Provider> {

    private AssetManager assets_manager;
    private Set<Provider> default_providers;
    private Set<Provider> custom_providers;

    final protected static String URLS = "urls";

    public ProviderManager(AssetManager assets_manager) {
        this.assets_manager = assets_manager;
        default_providers = default_providers();
        custom_providers = new HashSet<Provider>();
    }

    /**
     * Loads providers data from url files contained in the assets folder
     * @return true if the files were correctly read
     */
    private Set<Provider> default_providers() {
        Set<Provider> providers = new HashSet<Provider>();
        try {
            for(String file : assets_manager.list(URLS)) {
                String main_url = extractProviderMainUrlFromAssetsFile(URLS + "/" + file);
                providers.add(new Provider(new URL(main_url)));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return providers;
    }

    private String extractProviderMainUrlFromAssetsFile(String file_path) {
        String provider_main_url = "";
        try {
            InputStream input_stream_file_contents = assets_manager.open(file_path);
            byte[] urls_file_bytes = new byte[input_stream_file_contents.available()];
            if(input_stream_file_contents.read(urls_file_bytes) > 0) {
                String urls_file_content = new String(urls_file_bytes);
                JSONObject file_contents = new JSONObject(urls_file_content);
                provider_main_url = file_contents.getString(Provider.MAIN_URL);
            }
        } catch (JSONException e) {
        } catch (IOException e) {
        }
        return provider_main_url;
    }

    public Set<Provider> providers() {
        Set<Provider> all_providers = new HashSet<Provider>();
        all_providers.addAll(default_providers);
        all_providers.addAll(custom_providers);
        return all_providers;
    }

    @Override
    public int size() {
        return providers().size();
    }

    @Override
    public Provider get(int index) {
        Iterator<Provider> iterator = providers().iterator();
        while(iterator.hasNext() && index > 0) {
            iterator.next();
            index--;
        }
        return iterator.next();
    }

    @Override
    public void add(Provider element) {
        custom_providers.add(element);
    }

    @Override
    public void remove(Provider element) {
        custom_providers.remove(element);
    }

    @Override
    public void addAll(Collection<Provider> elements) {
        custom_providers.addAll(elements);
    }

    @Override
    public void removeAll(Collection<Provider> elements) {
        custom_providers.removeAll(elements);
        default_providers.removeAll(elements);
    }

    @Override
    public void clear() {
        default_providers.clear();
        custom_providers.clear();
    }
}
