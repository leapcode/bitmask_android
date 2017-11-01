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
            instance = new ProviderManager(assets_manager, external_files_dir);

        return instance;
    }

    public ProviderManager(AssetManager assets_manager, File external_files_dir) {
        this.assets_manager = assets_manager;
        addDefaultProviders(assets_manager);
        addCustomProviders(external_files_dir);
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
                InputStream provider_file = assets_manager.open(directory + "/" + file);
                String main_url = extractMainUrlFromInputStream(provider_file);
                String certificate_pin = extractCertificatePinFromInputStream(provider_file);
                if(certificate_pin.isEmpty())
                    providers.add(new Provider(new URL(main_url)));
                else
                    providers.add(new Provider(new URL(main_url), certificate_pin));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return providers;
    }


    private void addCustomProviders(File external_files_dir) {
        this.external_files_dir = external_files_dir;
        custom_providers = external_files_dir != null && external_files_dir.isDirectory() ?
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
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }

        return providers;
    }

    private String extractMainUrlFromInputStream(InputStream input_stream) {
        String main_url = "";

        JSONObject file_contents = inputStreamToJson(input_stream);
        if(file_contents != null)
            main_url = file_contents.optString(Provider.MAIN_URL);
        return main_url;
    }

    private String extractCertificatePinFromInputStream(InputStream input_stream) {
        String certificate_pin = "";

        JSONObject file_contents = inputStreamToJson(input_stream);
        if(file_contents != null)
            certificate_pin = file_contents.optString(Provider.CA_CERT_FINGERPRINT);

        return certificate_pin;
    }

    private JSONObject inputStreamToJson(InputStream input_stream) {
        JSONObject json = null;
        try {
            byte[] bytes = new byte[input_stream.available()];
            if (input_stream.read(bytes) > 0)
                json = new JSONObject(new String(bytes));
            input_stream.reset();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public Set<Provider> providers() {
        Set<Provider> all_providers = new HashSet<Provider>();
        all_providers.addAll(default_providers);
        if(custom_providers != null)
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
    public boolean add(Provider element) {
        if (!default_providers.contains(element))
            return custom_providers.add(element);
        else return true;
    }

    @Override
    public boolean remove(Object element) {
        return custom_providers.remove(element);
    }

    @Override
    public boolean addAll(Collection<? extends Provider> elements) {
        return custom_providers.addAll(elements);
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        if(!elements.getClass().equals(Provider.class))
            return false;
        return default_providers.removeAll(elements) || custom_providers.removeAll(elements);
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
