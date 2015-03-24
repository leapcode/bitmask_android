package se.leap.bitmaskclient;

import android.content.res.*;

import com.pedrogomez.renderers.*;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderManager implements AdapteeCollection<Provider> {

    private AssetManager assets_manager;
    private File external_files_dir;
    private Set<Provider> default_providers;
    private Set<Provider> custom_providers;

    private static ProviderManager instance;

    final protected static String URLS = "urls";

    public static ProviderManager getInstance(AssetManager assets_manager, File external_files_dir) {
        if (instance == null)
            instance = new ProviderManager(assets_manager);

        instance.addCustomProviders(external_files_dir);
        return instance;
    }

    public ProviderManager(AssetManager assets_manager) {
        this.assets_manager = assets_manager;
        addDefaultProviders(assets_manager);
    }

    private void addDefaultProviders(AssetManager assets_manager) {
        try {
            default_providers = providersFromAssets(URLS, assets_manager.list(URLS));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Provider> providersFromAssets(String directory, String[] relative_file_paths) {
        Set<Provider> providers = new HashSet<Provider>();
        try {
            for (String file : relative_file_paths) {
                String main_url = extractMainUrlFromInputStream(assets_manager.open(directory + "/" + file));
                providers.add(new Provider(new URL(main_url)));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return providers;
    }


    private void addCustomProviders(File external_files_dir) {
        this.external_files_dir = external_files_dir;
        custom_providers = external_files_dir.isDirectory() ?
                providersFromFiles(external_files_dir.list()) :
                new HashSet<Provider>();
    }

    private Set<Provider> providersFromFiles(String[] files) {
        Set<Provider> providers = new HashSet<Provider>();
        try {
            for (String file : files) {
                String main_url = extractMainUrlFromInputStream(new FileInputStream(external_files_dir.getAbsolutePath() + "/" + file));
                providers.add(new Provider(new URL(main_url)));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return providers;
    }

    private String extractMainUrlFromInputStream(InputStream input_stream_file_contents) {
        String main_url = "";
        byte[] bytes = new byte[0];
        try {
            bytes = new byte[input_stream_file_contents.available()];
            if (input_stream_file_contents.read(bytes) > 0) {
                JSONObject file_contents = new JSONObject(new String(bytes));
                main_url = file_contents.getString(Provider.MAIN_URL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return main_url;
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
        while (iterator.hasNext() && index > 0) {
            iterator.next();
            index--;
        }
        return iterator.next();
    }

    @Override
    public void add(Provider element) {
        if (!default_providers.contains(element))
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

    protected void saveCustomProvidersToFile() {
        try {
            for (Provider provider : custom_providers) {
                File provider_file = new File(external_files_dir, provider.getName() + ".json");
                if (!provider_file.exists()) {
                    FileWriter writer = new FileWriter(provider_file);
                    writer.write(provider.toJson().toString());
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
