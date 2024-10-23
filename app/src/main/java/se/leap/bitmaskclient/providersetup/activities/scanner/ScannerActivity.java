package se.leap.bitmaskclient.providersetup.activities.scanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.leap.bitmaskclient.base.models.Introducer;
import se.leap.bitmaskclient.databinding.AScannerBinding;

@OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class ScannerActivity extends AppCompatActivity {
    public static final String INVITE_CODE = "invite_code";
    private static final String TAG = ScannerActivity.class.getSimpleName();
    private AScannerBinding binding;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisUseCase;

    public static Intent newIntent(Context context) {
        return new Intent(context, ScannerActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCamera();
    }

    private void initCamera() {
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ScannerViewModel.class).getProcessCameraProvider().observe(this, cameraProvider -> {
            this.cameraProvider = cameraProvider;
            bindCameraUseCases();
        });
    }

    private void bindCameraUseCases() {
        bindPreviewUseCase();
        bindAnalyseUseCase();
    }


    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        previewUseCase = new Preview.Builder().setTargetRotation(binding.previewView.getDisplay().getRotation()).build();
        previewUseCase.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception exception) {
            Log.e(TAG, exception.getMessage());
        }
    }


    private void bindAnalyseUseCase() {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        // BarcodeScannerOptions.Builder()
        //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        //     .build();
        BarcodeScanner barcodeScanner = BarcodeScanning.getClient();

        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        analysisUseCase = new ImageAnalysis.Builder().setTargetRotation(binding.previewView.getDisplay().getRotation()).build();

        // Initialize our background executor
        ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

        analysisUseCase.setAnalyzer(cameraExecutor, imageProxy -> {
            // Insert barcode scanning code here
            processImageProxy(barcodeScanner, imageProxy);
        });

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception exception) {
            Log.e(TAG, exception.getMessage());
        }
    }

    private void processImageProxy(BarcodeScanner barcodeScanner, ImageProxy imageProxy) {
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage).addOnSuccessListener(barcodes -> {
            for (Barcode barcode : barcodes) {
                try {
                    Introducer introducer = Introducer.fromUrl(barcode.getRawValue());
                    if (introducer != null && introducer.validate()) {
                        imageProxy.close();
                        setResult(RESULT_OK, new Intent().putExtra(INVITE_CODE, introducer));
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid introducer", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(this, "Invalid introducer", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, e.getMessage());
        }).addOnCompleteListener(task -> {
            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            imageProxy.close();
        });
    }
}
