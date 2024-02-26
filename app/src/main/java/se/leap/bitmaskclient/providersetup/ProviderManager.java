package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.base.models.Constants.EXT_JSON;
import static se.leap.bitmaskclient.base.models.Constants.EXT_PEM;
import static se.leap.bitmaskclient.base.models.Constants.URLS;
import static se.leap.bitmaskclient.base.models.Provider.GEOIP_URL;
import static se.leap.bitmaskclient.base.models.Provider.MAIN_URL;
import static se.leap.bitmaskclient.base.models.Provider.MOTD_URL;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_API_IP;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_IP;
import static se.leap.bitmaskclient.base.utils.InputStreamHelper.inputStreamToJson;
import static se.leap.bitmaskclient.base.utils.InputStreamHelper.loadInputStreamAsString;

import android.content.res.AssetManager;

import androidx.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderManager {

    private final AssetManager assetsManager;
    private Set<Provider> defaultProviders;
    // key: MainURL String, value: Provider
    private HashMap<String, Provider> customProviders;
    private Set<String> defaultProviderURLs;

    private static ProviderManager instance;
    private boolean addDummyEntry = false;

    public static ProviderManager getInstance(AssetManager assetsManager) {
        if (instance == null)
            instance = new ProviderManager(assetsManager);

        return instance;
    }

    @VisibleForTesting
    static void reset() {
        instance = null;
    }

    public void setAddDummyEntry(boolean addDummyEntry) {
        this.addDummyEntry = addDummyEntry;
    }

    private ProviderManager(AssetManager assetManager) {
        this.assetsManager = assetManager;
        addDefaultProviders(assetManager);
        addCustomProviders();
    }

    private void addDefaultProviders(AssetManager assetManager) {
        try {
            defaultProviders = providersFromAssets(URLS, assetManager.list(URLS));
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
                    JSONObject providerConfig = inputStreamToJson(providerFile);
                    if (providerConfig != null) {
                        mainUrl = providerConfig.optString(MAIN_URL);
                        providerIp = providerConfig.optString(PROVIDER_IP);
                        providerApiIp = providerConfig.optString(PROVIDER_API_IP);
                        geoipUrl =  providerConfig.optString(GEOIP_URL);
                        motdUrl = providerConfig.optString(MOTD_URL);
                    }
                    certificate = loadInputStreamAsString(assetsManager.open(provider + EXT_PEM));
                    providerDefinition = loadInputStreamAsString(assetsManager.open(provider + EXT_JSON));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                providers.add(new Provider(mainUrl, geoipUrl, motdUrl, providerIp, providerApiIp, certificate, providerDefinition));
            }

        return providers;
    }


    private void addCustomProviders() {
        customProviders = PreferenceHelper.getCustomProviders();
    }

    public List<Provider> providers() {
       return providers(addDummyEntry);
    }

    private List<Provider> providers(boolean addEmptyProvider) {
        List<Provider> allProviders = new ArrayList<>();
        allProviders.addAll(defaultProviders);
        if(customProviders != null)
            allProviders.addAll(customProviders.values());
        if (addEmptyProvider) {
            //add an option to add a custom provider
            allProviders.add(new Provider());
        }
        return allProviders;
    }

    public int size() {
        return providers().size();
    }

    public Provider get(int index) {
        Iterator<Provider> iterator = providers().iterator();
        while (iterator.hasNext() && index > 0) {
            iterator.next();
            index--;
        }
        return iterator.next();
    }

    public boolean add(Provider element) {
        boolean addElement = element != null &&
                !defaultProviderURLs.contains(element.getMainUrlString()) &&
                !customProviders.containsKey(element.getMainUrlString());
        if (addElement) {
            customProviders.put(element.getMainUrlString(), element);
            return true;
        }
        return false;
    }

    public boolean remove(Object element) {
        return element instanceof Provider &&
                customProviders.remove(((Provider) element).getMainUrlString()) != null;
    }

    public boolean addAll(Collection<? extends Provider> elements) {
        Iterator iterator = elements.iterator();
        boolean addedAll = true;
        while (iterator.hasNext()) {
            Provider p = (Provider) iterator.next();
            boolean containsKey = customProviders.containsKey(p.getMainUrlString());
            if (!containsKey) {
                customProviders.put(p.getMainUrlString(), p);
            }
            addedAll = !containsKey && addedAll;
        }
        return addedAll;
    }

    public boolean removeAll(Collection<?> elements) {
        Iterator iterator = elements.iterator();
        boolean removedAll = true;
        while (iterator.hasNext()) {
            removedAll = remove(iterator.next()) && removedAll;
        }

        return removedAll;
    }

    public void clear() {
        defaultProviders.clear();
        customProviders.clear();
        defaultProviderURLs.clear();
    }

    public void saveCustomProviders() {
        PreferenceHelper.setCustomProviders(new HashSet<>(customProviders.values()));
    }
}
