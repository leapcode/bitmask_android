package se.leap.bitmaskclient;

import android.content.res.AssetManager;

import com.pedrogomez.renderers.AdapteeCollection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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

    private static final String TAG = ProviderManager.class.getName();
    private AssetManager assets_manager;
    private File externalFilesDir;
    private Set<Provider> defaultProviders;
    private Set<Provider> customProviders;

    private static ProviderManager instance;

    final protected static String URLS = "urls";

    public static ProviderManager getInstance(AssetManager assetsManager, File externalFilesDir) {
        if (instance == null)
            instance = new ProviderManager(assetsManager, externalFilesDir);

        return instance;
    }

    public ProviderManager(AssetManager assetManager, File externalFilesDir) {
        this.assets_manager = assetManager;
        addDefaultProviders(assetManager);
        addCustomProviders(externalFilesDir);
    }

    private void addDefaultProviders(AssetManager assets_manager) {
        try {
            defaultProviders = providersFromAssets(URLS, assets_manager.list(URLS));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Provider> providersFromAssets(String directory, String[] relativeFilePaths) {
        Set<Provider> providers = new HashSet<Provider>();

            for (String file : relativeFilePaths) {
                String mainUrl = null;
                String certificate = null;
                String providerDefinition = null;
                try {
                    String provider = file.substring(0, file.length() - ".url".length());
                    InputStream provider_file = assets_manager.open(directory + "/" + file);
                    mainUrl = extractMainUrlFromInputStream(provider_file);
                    certificate = ConfigHelper.loadInputStreamAsString(assets_manager.open(provider + ".pem"));
                    providerDefinition = ConfigHelper.loadInputStreamAsString(assets_manager.open(provider + ".json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    providers.add(new Provider(new URL(mainUrl), certificate, providerDefinition));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

        return providers;
    }


    private void addCustomProviders(File externalFilesDir) {
        this.externalFilesDir = externalFilesDir;
        customProviders = externalFilesDir != null && externalFilesDir.isDirectory() ?
                providersFromFiles(externalFilesDir.list()) :
                new HashSet<Provider>();
    }

    private Set<Provider> providersFromFiles(String[] files) {
        Set<Provider> providers = new HashSet<Provider>();
        try {
            for (String file : files) {
                String main_url = extractMainUrlFromInputStream(new FileInputStream(externalFilesDir.getAbsolutePath() + "/" + file));
                providers.add(new Provider(new URL(main_url)));
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }

        return providers;
    }

    private String extractMainUrlFromInputStream(InputStream input_stream) {
        String mainUrl = "";

        JSONObject file_contents = inputStreamToJson(input_stream);
        if (file_contents != null)
            mainUrl = file_contents.optString(Provider.MAIN_URL);
        return mainUrl;
    }

    private JSONObject inputStreamToJson(InputStream inputStream) {
        JSONObject json = null;
        try {
            byte[] bytes = new byte[inputStream.available()];
            if (inputStream.read(bytes) > 0)
                json = new JSONObject(new String(bytes));
            inputStream.reset();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public Set<Provider> providers() {
        Set<Provider> all_providers = new HashSet<Provider>();
        all_providers.addAll(defaultProviders);
        if(customProviders != null)
            all_providers.addAll(customProviders);
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
        if (!defaultProviders.contains(element))
            return customProviders.add(element);
        else return true;
    }

    @Override
    public boolean remove(Object element) {
        return customProviders.remove(element);
    }

    @Override
    public boolean addAll(Collection<? extends Provider> elements) {
        return customProviders.addAll(elements);
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        if(!elements.getClass().equals(Provider.class))
            return false;
        return defaultProviders.removeAll(elements) || customProviders.removeAll(elements);
    }

    @Override
    public void clear() {
        defaultProviders.clear();
        customProviders.clear();
    }

    protected void saveCustomProvidersToFile() {
        try {
            for (Provider provider : customProviders) {
                File providerFile = new File(externalFilesDir, provider.getName() + ".json");
                if (!providerFile.exists()) {
                    FileWriter writer = new FileWriter(providerFile);
                    writer.write(provider.toJson().toString());
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
