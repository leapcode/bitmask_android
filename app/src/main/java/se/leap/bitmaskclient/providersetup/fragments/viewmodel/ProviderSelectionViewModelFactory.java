package se.leap.bitmaskclient.providersetup.fragments.viewmodel;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ProviderSelectionViewModelFactory implements ViewModelProvider.Factory {
    private final AssetManager assetManager;

    public ProviderSelectionViewModelFactory(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProviderSelectionViewModel.class)) {
            return (T) new ProviderSelectionViewModel(assetManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}