package com.example.przestrzeliny_app;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ImageButton btnCapture;
    private ImageCapture imageCapture = null;
    private ExecutorService cameraExecutor;
    private SeekBar zoomSlider;
    private Camera camera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        zoomSlider = findViewById(R.id.zoomSlider);

        // 1. Znajdujemy celownik i pole na wynik
        Button btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        TextView txtResult = findViewById(R.id.txtResult);

        // klikniecie przełacza obiektyw
        btnSwitchCamera.setOnClickListener(v -> {
            cameraIndex++; // Zwiększamy indeks
            startCamera(); // Restartujemy kamerę
            Toast.makeText(this, "Szukam następnego obiektywu...", Toast.LENGTH_SHORT).show();
        });
        // -----------------------

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btnCapture.setOnClickListener(v -> captureImage());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int cameraIndex = 0; //licznik obiektywów
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // POBIERAMY LISTĘ WSZYSTKICH DOSTĘPNYCH KAMER (Punkt 1)
                List<CameraInfo> availableCameras = cameraProvider.getAvailableCameraInfos();
                if (availableCameras.isEmpty()) return;

                // Jeśli mamy np. 3 obiektywy, pilnujemy by nie wyjść poza listę
                if (cameraIndex >= availableCameras.size()) cameraIndex = 0;

                // Wybieramy konkretny obiektyw z listy
                CameraSelector cameraSelector = availableCameras.get(cameraIndex).getCameraSelector();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                cameraProvider.unbindAll();
                zoomSlider.setProgress(0);
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                initZoomSlider();

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Błąd startu kamery", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void initZoomSlider() {
        zoomSlider.setMax(100); // Upewniamy się, że zakres to 0-100
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Reagujemy tylko, gdy to użytkownik przesuwa (fromUser)
                // i gdy mamy aktywny obiekt kamery
                if (fromUser && camera != null) {
                    try {
                        // Pobieramy aktualny stan zooma z kamery
                        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                        if (zoomState != null) {
                            // Skalujemy progress (0.0 - 1.0)
                            float linearZoom = (progress / 100f)*0.4f;

                            // Bardzo ważne: setLinearZoom jest bezpieczniejsze niż setZoomRatio
                            camera.getCameraControl().setLinearZoom(linearZoom);
                        }
                    } catch (Exception e) {
                        Log.e("ZoomError", "Błąd sprzętowy zooma: " + e.getMessage());
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void captureImage() {
        if (imageCapture == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + timestamp + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Images");
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "Image saved to gallery!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };
}