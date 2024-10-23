package se.leap.bitmaskclient.providersetup.activities.scanner;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class ScannerViewModel extends AndroidViewModel {
    private static final String TAG = ScannerViewModel.class.getSimpleName();
    private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;

    public ScannerViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
        if (cameraProviderLiveData == null) {
            cameraProviderLiveData = new MutableLiveData<>();
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication());
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProviderLiveData.setValue(cameraProviderFuture.get());
                } catch (ExecutionException e) {
                    // Handle any errors (including cancellation) here.
                    Log.e(TAG, "Unhandled exception", e);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Unhandled exception", e);
                }
            }, ContextCompat.getMainExecutor(getApplication()));
        }
        return cameraProviderLiveData;
    }
}
