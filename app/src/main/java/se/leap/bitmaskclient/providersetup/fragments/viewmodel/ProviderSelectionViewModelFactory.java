package se.leap.bitmaskclient.providersetup.fragments.viewmodel;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;

public class ProviderSelectionViewModelFactory implements ViewModelProvider.Factory {
    private final AssetManager assetManager;
    private final File externalFilesDir;

    public ProviderSelectionViewModelFactory(AssetManager assetManager, File externalFilesDir) {
        this.assetManager = assetManager;
        this.externalFilesDir = externalFilesDir;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProviderSelectionViewModel.class)) {
            return (T) new ProviderSelectionViewModel(assetManager, externalFilesDir);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}