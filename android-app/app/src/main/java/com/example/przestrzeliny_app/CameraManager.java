package com.example.przestrzeliny_app;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public class CameraManager {

    private final AppCompatActivity activity;
    private final PreviewView viewFinder;
    private final SeekBar zoomSlider;
    private int cameraIndex=0;
    private ImageCapture imageCapture;
    private Camera camera;


    public CameraManager(AppCompatActivity activity, PreviewView viewFinder, SeekBar zoomSlider) {
        this.activity = activity;
        this.viewFinder = viewFinder;
        this.zoomSlider = zoomSlider;
    }
    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                List<CameraInfo> availableCameras = cameraProvider.getAvailableCameraInfos();

                if (availableCameras.isEmpty()) return;
                if (cameraIndex >= availableCameras.size()) cameraIndex = 0;

                CameraSelector cameraSelector = availableCameras.get(cameraIndex).getCameraSelector();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                cameraProvider.unbindAll();
                zoomSlider.setProgress(0);
                camera = cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture);

                initZoomSlider();

            } catch (Exception e) {
                Log.e("CameraX", "Błąd startu kamery: " + e.getMessage());
                Toast.makeText(activity, "Błąd startu kamery", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(activity));
    }
    private void initZoomSlider() {
        zoomSlider.setMax(100);
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && camera != null) {
                    float linearZoom = (progress / 100f) * 0.4f;
                    camera.getCameraControl().setLinearZoom(linearZoom);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    public void switchCamera() {
        cameraIndex++;
        startCamera(); // Po zwiększeniu indeksu, menedżer sam restartuje kamerę
    }

    // jesli MainActivity chce zdj prosi CameraManagera o udostępnienie obiektu ImageCapture.
    public ImageCapture getImageCapture() {
        return imageCapture;
    }
}

