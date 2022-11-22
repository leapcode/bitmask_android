package se.leap.bitmaskclient.providersetup;

import android.content.res.AssetManager;

import androidx.annotation.VisibleForTesting;

import com.pedrogomez.renderers.AdapteeCollection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import se.leap.bitmaskclient.base.models.Provider;

import static se.leap.bitmaskclient.base.models.Constants.EXT_JSON;
import static se.leap.bitmaskclient.base.models.Constants.EXT_PEM;
import static se.leap.bitmaskclient.base.models.Constants.URLS;
import static se.leap.bitmaskclient.base.models.Provider.GEOIP_URL;
import static se.leap.bitmaskclient.base.models.Provider.MAIN_URL;
import static se.leap.bitmaskclient.base.models.Provider.MOTD_URL;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_API_IP;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_IP;
import static se.leap.bitmaskclient.base.utils.FileHelper.createFile;
import static se.leap.bitmaskclient.base.utils.FileHelper.persistFile;
import static se.leap.bitmaskclient.base.utils.InputStreamHelper.extractKeyFromInputStream;
import static se.leap.bitmaskclient.base.utils.InputStreamHelper.getInputStreamFrom;
import static se.leap.bitmaskclient.base.utils.InputStreamHelper.loadInputStreamAsString;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderManager implements AdapteeCollection<Provider> {

    private AssetManager assetsManager;
    private File externalFilesDir;
    private Set<Provider> defaultProviders;
    private Set<Provider> customProviders;
    private Set<String> defaultProviderURLs;
    private Set<String> customProviderURLs;

    private static ProviderManager instance;

    public static ProviderManager getInstance(AssetManager assetsManager, File externalFilesDir) {
        if (instance == null)
            instance = new ProviderManager(assetsManager, externalFilesDir);

        return instance;
    }

    @VisibleForTesting
    static void reset() {
        instance = null;
    }

    private ProviderManager(AssetManager assetManager, File externalFilesDir) {
        this.assetsManager = assetManager;
        addDefaultProviders(assetManager);
        addCustomProviders(externalFilesDir);
    }

    private void addDefaultProviders(AssetManager assets_manager) {
        try {
            defaultProviders = providersFromAssets(URLS, assets_manager.list(URLS));
            defaultProviderURLs = getProviderUrlSetFromProviderSet(defaultProviders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> getProviderUrlSetFromProviderSet(Set<Provider> providers) {
        HashSet<String> providerUrls = new HashSet<>();
        for (Provider provider : providers) {
            providerUrls.add(provider.getMainUrl().toString());
        }
        return providerUrls;
    }

    private Set<Provider> providersFromAssets(String directory, String[] relativeFilePaths) {
        Set<Provider> providers = new HashSet<>();

            for (String file : relativeFilePaths) {
                String mainUrl = null;
                String providerIp = null;
                String providerApiIp = null;
                String certificate = null;
                String providerDefinition = null;
                String geoipUrl = null;
                String motdUrl = null;
                try {
                    String provider = file.substring(0, file.length() - ".url".length());
                    InputStream providerFile = assetsManager.open(directory + "/" + file);
                    mainUrl = extractKeyFromInputStream(providerFile, MAIN_URL);
                    providerIp = extractKeyFromInputStream(providerFile, PROVIDER_IP);
                    providerApiIp = extractKeyFromInputStream(providerFile, PROVIDER_API_IP);
                    geoipUrl =  extractKeyFromInputStream(providerFile, GEOIP_URL);
                    motdUrl = extractKeyFromInputStream(providerFile, MOTD_URL);
                    certificate = loadInputStreamAsString(assetsManager.open(provider + EXT_PEM));
                    providerDefinition = loadInputStreamAsString(assetsManager.open(provider + EXT_JSON));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                providers.add(new Provider(mainUrl, geoipUrl, motdUrl, providerIp, providerApiIp, certificate, providerDefinition));
            }

        return providers;
    }


    private void addCustomProviders(File externalFilesDir) {
        this.externalFilesDir = externalFilesDir;
        customProviders = externalFilesDir != null && externalFilesDir.isDirectory() ?
                providersFromFiles(externalFilesDir.list()) :
                new HashSet<>();
        customProviderURLs = getProviderUrlSetFromProviderSet(customProviders);
    }

    private Set<Provider> providersFromFiles(String[] files) {
        Set<Provider> providers = new HashSet<>();
        try {
            for (String file : files) {
                InputStream inputStream = getInputStreamFrom(externalFilesDir.getAbsolutePath() + "/" + file);
                String mainUrl = extractKeyFromInputStream(inputStream, MAIN_URL);
                String providerIp = extractKeyFromInputStream(inputStream, PROVIDER_IP);
                String providerApiIp = extractKeyFromInputStream(inputStream, PROVIDER_API_IP);
                providers.add(new Provider(mainUrl, providerIp, providerApiIp));
            }
        } catch (FileNotFoundException | NullPointerException e) {
            e.printStackTrace();
        }

        return providers;
    }

    public List<Provider> providers() {
        List<Provider> allProviders = new ArrayList<>();
        allProviders.addAll(defaultProviders);
        if(customProviders != null)
            allProviders.addAll(customProviders);
        //add an option to add a custom provider
        //TODO: refactor me?
        allProviders.add(new Provider());
        return allProviders;
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
        return element != null &&
                !defaultProviderURLs.contains(element.getMainUrl().toString()) &&
                customProviders.add(element) &&
                customProviderURLs.add(element.getMainUrl().toString());
    }

    @Override
    public boolean remove(Object element) {
        return element instanceof Provider &&
                customProviders.remove(element) &&
                customProviderURLs.remove(((Provider) element).getMainUrl().toString());
    }

    @Override
    public boolean addAll(Collection<? extends Provider> elements) {
        Iterator iterator = elements.iterator();
        boolean addedAll = true;
        while (iterator.hasNext()) {
            Provider p = (Provider) iterator.next();
            addedAll = customProviders.add(p) &&
                    customProviderURLs.add(p.getMainUrl().toString()) &&
                    addedAll;
        }
        return addedAll;
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        Iterator iterator = elements.iterator();
        boolean removedAll = true;
        try {
            while (iterator.hasNext()) {
                Provider p = (Provider) iterator.next();
                removedAll = ((defaultProviders.remove(p) && defaultProviderURLs.remove(p.getMainUrl().toString())) ||
                        (customProviders.remove(p) && customProviderURLs.remove(p.getMainUrl().toString()))) &&
                        removedAll;
            }
        } catch (ClassCastException e) {
            return false;
        }

        return removedAll;
    }

    @Override
    public void clear() {
        defaultProviders.clear();
        customProviders.clear();
        customProviderURLs.clear();
        defaultProviderURLs.clear();
    }

    void saveCustomProvidersToFile() {
        try {
            deleteLegacyCustomProviders();

            for (Provider provider : customProviders) {
                File providerFile = createFile(externalFilesDir, provider.getName() + EXT_JSON);
                if (!providerFile.exists()) {
                    persistFile(providerFile, provider.toJson().toString());
                }
            }
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes persisted custom providers from from internal storage that are not in customProviders list anymore
     */
    private void deleteLegacyCustomProviders() throws IOException, SecurityException {
        Set<Provider> persistedCustomProviders = externalFilesDir != null && externalFilesDir.isDirectory() ?
                providersFromFiles(externalFilesDir.list()) : new HashSet<Provider>();
            persistedCustomProviders.removeAll(customProviders);
        for (Provider providerToDelete : persistedCustomProviders) {
            File providerFile = createFile(externalFilesDir, providerToDelete.getName() + EXT_JSON);
            if (providerFile.exists()) {
                providerFile.delete();
            }
        }
    }
}
