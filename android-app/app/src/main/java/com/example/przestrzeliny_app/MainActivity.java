package com.example.przestrzeliny_app;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
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
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ImageButton btnCapture;
    private SeekBar zoomSlider;
    private Button btnSwitchCamera;
    private TextView txtResult;

    // Nasi pomocnicy
    private CameraManager cameraManager;
    private PermissionHelper permissionHelper;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Uruchamiamy silnik OpenCV (ZAKOMENTUJ TĘ LINIJKĘ JEŚLI UŻYWASZ EMULATORA API 36)
        if (OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCV załadowane i czeka w gotowości!");
        } else {
            Log.e("OPENCV", "Błąd ładowania OpenCV");
            Toast.makeText(this, "Błąd biblioteki graficznej", Toast.LENGTH_SHORT).show();
        }

        // 2. Łączymy widoki z XML
        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        zoomSlider = findViewById(R.id.zoomSlider);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        txtResult = findViewById(R.id.txtResult); //wyniki YOLO w przyszłości

        // 3. Inicjalizacja Menedżera Kamery
        cameraManager = new CameraManager(this, viewFinder, zoomSlider);

        // 4. Obsługa zmiany obiektywu
        btnSwitchCamera.setOnClickListener(v -> {
            cameraManager.switchCamera();
            Toast.makeText(this, "Szukam następnego obiektywu...", Toast.LENGTH_SHORT).show();
        });

        // 5. Inicjalizacja Pomocnika od uprawnień z "Krótkofalówką"
        permissionHelper = new PermissionHelper(this, new PermissionHelper.PermissionListener() {
            @Override
            public void onPermissionsGranted() {
                cameraManager.startCamera(); // Mamy zgodę -> Odpalamy kamerę
            }

            @Override
            public void onPermissionsDenied() {
                Toast.makeText(MainActivity.this, "Brak uprawnień do kamery.", Toast.LENGTH_SHORT).show();
                finish(); // Brak zgody -> Wyłączamy apkę
            }
        });

        // 6. Sprawdzamy uprawnienia przy starcie
        if (permissionHelper.allPermissionsGranted()) {
            cameraManager.startCamera();
        } else {
            permissionHelper.requestPermissions();
        }

        // 7. Podpięcie przycisku robienia zdjęć
        btnCapture.setOnClickListener(v -> takePhotoAndProcess());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhotoAndProcess() {
        ImageCapture currentImageCapture = cameraManager.getImageCapture();
        if (currentImageCapture == null) return;

        currentImageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    Bitmap rawBitmap = imageProxyToBitmap(image);
                    int rotationDegrees = image.getImageInfo().getRotationDegrees();

                    // Obrót obrazu
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);

                    // --- INTELIGENTNY CROP I SKALOWANIE ---
                    int width = rotatedBitmap.getWidth();
                    height = rotatedBitmap.getHeight();
                    int minSide = Math.min(width, height); // Wybiera krótszy bok (szerokość telefonu)

                    // Wyliczamy środek
                    int startX = (width - minSide) / 2;
                    int startY = (height - minSide) / 2;

                    // 1. Wycinamy idealny kwadrat
                    Bitmap squareBitmap = Bitmap.createBitmap(rotatedBitmap, startX, startY, minSide, minSide);

                    // 2. Skalujemy do wymogów modelu AI (2048x2048)
                    int targetSize = 2048;
                    Bitmap croppedAndScaledBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true);

                    // Zapisujemy gotowy obraz do galerii
                    saveBitmapToGallery(croppedAndScaledBitmap);
                    Toast.makeText(MainActivity.this, "Wycięto i przeskalowano (2048x2048)!", Toast.LENGTH_SHORT).show();

                    // Optymalizacja pamięci - sprzątamy po sobie
                    rawBitmap.recycle();
                    rotatedBitmap.recycle();
                    squareBitmap.recycle();

                    // TUTAJ W PRZYSZŁOŚCI WYŚLEMY croppedAndScaledBitmap DO NCNN

                } catch (Exception e) {
                    Log.e("ImageProcess", "Błąd przy obróbce: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Błąd obróbki obrazu!", Toast.LENGTH_SHORT).show();
                } finally {
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Błąd robienia zdjęcia: " + exception.getMessage());
                Toast.makeText(MainActivity.this, "Aparat nie złapał klatki", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues contentValues = new ContentValues();

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "AI_CROP_" + timestamp + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Przestrzeliny");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                Log.e("Zapis", "Błąd zapisu wycinka: " + e.getMessage());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Zlecamy Pomocnikowi analizę wyników
        permissionHelper.handlePermissionsResult(requestCode, grantResults);
    }
}