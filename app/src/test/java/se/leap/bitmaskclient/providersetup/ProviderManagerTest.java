package se.leap.bitmaskclient.providersetup;

import android.content.res.AssetManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.FileHelper;
import se.leap.bitmaskclient.base.utils.InputStreamHelper;
import se.leap.bitmaskclient.providersetup.ProviderManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static se.leap.bitmaskclient.testutils.MockHelper.mockFileHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockInputStreamHelper;

/**
 * Created by cyberta on 20.02.18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigHelper.class, FileHelper.class, InputStreamHelper.class})
public class ProviderManagerTest {

    @Mock
    private AssetManager assetManager;
    @Mock
    private File file;
    private ProviderManager providerManager;

    @Before
    public void setup() throws Exception {
        //mock assetManager methods
        //--------------------------
        when(assetManager.open(anyString())).thenAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                String filename = "preconfigured/" + invocation.getArguments()[0];
                return getClass().getClassLoader().getResourceAsStream(filename);
            }
        });
        when(assetManager.list(anyString())).thenAnswer(new Answer<String[]>() {
            @Override
            public String[] answer(InvocationOnMock invocation) throws Throwable {
                String path = (String) invocation.getArguments()[0];
                if ("urls".equals(path)) {
                    String[] preconfiguredUrls = new String[3];
                    preconfiguredUrls[0] = "calyx.net.url";
                    preconfiguredUrls[1] = "demo.bitmask.net.url";
                    preconfiguredUrls[2] = "riseup.net.url";
                    return preconfiguredUrls;
                } else
                    throw new IllegalArgumentException("You need to implement the expected path manually!");
            }
        });

        //mock File methods
        //------------------
        when(file.isDirectory()).thenReturn(true);

        ArrayList<String> mockedCustomProviderList = new ArrayList<>();
        mockedCustomProviderList.add("leapcolombia.json");
        String[] mockedCustomProviderArray = new String[mockedCustomProviderList.size()];
        mockedCustomProviderArray = mockedCustomProviderList.toArray(mockedCustomProviderArray);
        when(file.list()).thenReturn(mockedCustomProviderArray);

        when(file.getAbsolutePath()).thenReturn("externalDir");
        when(file.getPath()).thenReturn("externalDir");
        mockFileHelper(file);

        // mock inputStream
        //-----------------------------------
        mockInputStreamHelper();

    }

    @After
    public void tearDown() {
        ProviderManager.reset();
    }

    @Test
    public void testSize_dummyEntry_has5ProvidersWithCurrentTestSetup() {
        providerManager = ProviderManager.getInstance(assetManager, file);
        providerManager.setAddDummyEntry(true);
        assertEquals("3 preconfigured, 1 custom provider, 1 dummy provider", 5, providerManager.size());
    }

    @Test
    public void testSize_has4ProvidersWithCurrentTestSetup() {
        providerManager = ProviderManager.getInstance(assetManager, file);
        assertEquals("3 preconfigured, 1 custom provider", 4, providerManager.size());
    }


    @Test
    public void testAdd_dummyEntry_newCustomProviderThatIsNotPartOfDefaultNorCustomList_returnTrue() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        providerManager.setAddDummyEntry(true);
        Provider customProvider = new Provider("https://anewprovider.org");
        assertTrue("custom provider added: ", providerManager.add(customProvider));
        assertEquals("3 preconfigured, 2 custom providers, 1 dummy provider", 6, providerManager.providers().size());
    }

    @Test
    public void testAdd_newCustomProviderThatIsNotPartOfDefaultNorCustomList_returnTrue() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://anewprovider.org");
        assertTrue("custom provider added: ", providerManager.add(customProvider));
        assertEquals("3 preconfigured, 2 custom providers", 5, providerManager.providers().size());
    }

    @Test
    public void testAdd_dummyEntry_newCustomProviderThatIsNotPartOfDefaultButOfCustomList_returnFalse() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        providerManager.setAddDummyEntry(true);
        Provider customProvider = new Provider("https://leapcolombia.org");
        assertFalse("custom provider added: ", providerManager.add(customProvider));
        assertEquals("3 preconfigured, 1 custom provider, 1 dummy provider", 5, providerManager.providers().size());
    }

    @Test
    public void testAdd_newCustomProviderThatIsNotPartOfDefaultButOfCustomList_returnFalse() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://leapcolombia.org");
        assertFalse("custom provider added: ", providerManager.add(customProvider));
        assertEquals("3 preconfigured, 1 custom provider", 4, providerManager.providers().size());
    }

    @Test
    public void testAdd_newCustomProviderThatIsPartOfDefaultButNotOfCustomList_returnFalse() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://demo.bitmask.net");
        assertFalse("custom provider added: ", providerManager.add(customProvider));
        assertEquals("3 preconfigured, 1 custom provider", 4, providerManager.providers().size());
    }

    @Test
    public void testRemove_ProviderIsPartOfDefaultButNotCustomList_returnsFalse() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://demo.bitmask.net");
        assertFalse("custom provider not removed: ", providerManager.remove(customProvider));
        assertEquals("3 preconfigured, 1 custom provider", 4, providerManager.providers().size());
    }

    @Test
    public void testRemove_ProviderIsNotPartOfDefaultButOfCustomList_returnsTrue() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://leapcolombia.org");
        assertTrue("custom provider not removed: ", providerManager.remove(customProvider));
        assertEquals("3 preconfigured, 0 custom providers", 3, providerManager.providers().size());
    }

    @Test
    public void testRemove_ProviderIsNotPartOfDefaultNorOfCustomList_returnsFalse() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://anotherprovider.org");
        assertFalse("custom provider not removed: ", providerManager.remove(customProvider));
        assertEquals("3 preconfigured, 1 custom providers", 4, providerManager.providers().size());
    }

    @Test
    public void testClear_dummyEntry_ProvidersListHasOnlyDummyProvider() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        providerManager.setAddDummyEntry(true);
        providerManager.clear();
        assertEquals("1 providers", 1, providerManager.providers().size());
        assertEquals("provider is dummy element", "https://example.net", providerManager.get(0).getMainUrlString());
    }

    @Test
    public void testClear_noEntries() throws Exception {
        providerManager = ProviderManager.getInstance(assetManager, file);
        providerManager.clear();
        assertEquals("no providers", 0, providerManager.providers().size());
    }

    @Test
    public void testSaveCustomProvidersToFile_CustomProviderDeleted_deletesFromDir() throws Exception {
        when(file.exists()).thenReturn(true);
        providerManager = ProviderManager.getInstance(assetManager, file);
        //leapcolombia is mocked custom provider from setup
        Provider customProvider = new Provider("https://leapcolombia.org");
        providerManager.remove(customProvider);
        providerManager.saveCustomProvidersToFile();
        verify(file, times(1)).delete();
    }


    @Test
    public void testSaveCustomProvidersToFile_newCustomProviders_persistNew() throws Exception {
        when(file.list()).thenReturn(new String[0]);
        when(file.exists()).thenReturn(false);
        providerManager = ProviderManager.getInstance(assetManager, file);
        Provider customProvider = new Provider("https://anotherprovider.org");
        Provider secondCustomProvider = new Provider("https://yetanotherprovider.org");
        providerManager.add(customProvider);
        providerManager.add(secondCustomProvider);
        providerManager.saveCustomProvidersToFile();

        verifyStatic(FileHelper.class, times(2));
        FileHelper.persistFile(any(File.class), anyString());
    }


}