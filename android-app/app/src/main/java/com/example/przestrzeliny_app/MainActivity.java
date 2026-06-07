package com.example.przestrzeliny_app;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private ImageView imageViewResult;

    // Nasi pomocnicy
    private CameraManager cameraManager;
    private PermissionHelper permissionHelper;
    private ExecutorService cameraExecutor;

    // Klasa odpowiedzialna za AI
    private YoloDetector yoloDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Uruchamiamy silnik OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCV załadowane i czeka w gotowości!");
        } else {
            Log.e("OPENCV", "Błąd ładowania OpenCV");
            Toast.makeText(this, "Błąd biblioteki graficznej", Toast.LENGTH_SHORT).show();
        }

        // Łączymy widoki z XML
        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        zoomSlider = findViewById(R.id.zoomSlider);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        txtResult = findViewById(R.id.txtResult);
        imageViewResult = findViewById(R.id.imageViewResult);

        // Inicjalizacja modelu AI
        yoloDetector = new YoloDetector();
        boolean isLoaded = yoloDetector.initModel(getAssets(), "model.ncnn.param", "model.ncnn.bin");
        if (!isLoaded) {
            Toast.makeText(this, "Błąd ładowania modelu AI!", Toast.LENGTH_LONG).show();
        }

        // Inicjalizacja Menedżera Kamery
        cameraManager = new CameraManager(this, viewFinder, zoomSlider);

        // Obsługa zmiany obiektywu
        btnSwitchCamera.setOnClickListener(v -> {
            cameraManager.switchCamera();
            Toast.makeText(this, "Szukam następnego obiektywu...", Toast.LENGTH_SHORT).show();
        });

        // Inicjalizacja Pomocnika od uprawnień
        permissionHelper = new PermissionHelper(this, new PermissionHelper.PermissionListener() {
            @Override
            public void onPermissionsGranted() {
                cameraManager.startCamera();
            }

            @Override
            public void onPermissionsDenied() {
                Toast.makeText(MainActivity.this, "Brak uprawnień do kamery.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Sprawdzamy uprawnienia przy starcie
        if (permissionHelper.allPermissionsGranted()) {
            cameraManager.startCamera();
        } else {
            permissionHelper.requestPermissions();
        }

        // Podpięcie przycisku robienia zdjęć
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
                    // Konwersja i obrót
                    Bitmap rawBitmap = imageProxyToBitmap(image);
                    int rotationDegrees = image.getImageInfo().getRotationDegrees();

                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);

                    // IDEALNY CROP (MATEMATYKA CAMERAX)
                    View viewFinder = findViewById(R.id.viewFinder);
                    View targetFrame = findViewById(R.id.targetFrame);

                    int w_img = rotatedBitmap.getWidth();
                    int h_img = rotatedBitmap.getHeight();
                    int w_view = viewFinder.getWidth();
                    int h_view = viewFinder.getHeight();

                    float scale = Math.max((float) w_view / w_img, (float) h_view / h_img);
                    int cropSize = (int) (targetFrame.getWidth() / scale);

                    int startX = Math.max(0, (w_img - cropSize) / 2);
                    int startY = Math.max(0, (h_img - cropSize) / 2);
                    if (startX + cropSize > w_img) cropSize = w_img - startX;
                    if (startY + cropSize > h_img) cropSize = h_img - startY;

                    Bitmap squareBitmap = Bitmap.createBitmap(rotatedBitmap, startX, startY, cropSize, cropSize);

                    // SKALOWANIE DLA AI (Musi być 2048)
                    int targetSize = 2048;
                    Bitmap croppedAndScaledBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true);

                    // Zapisujemy wycięty ideał do galerii (opcjonalnie)
                    // saveBitmapToGallery(croppedAndScaledBitmap);

                    // PRZESYŁAMY DO AI
                    Detection[] wyniki = yoloDetector.processImage(croppedAndScaledBitmap);

                    if (wyniki != null && wyniki.length > 0) {
                        StringBuilder raport = new StringBuilder();
                        raport.append("ZNALAZŁEM ").append(wyniki.length).append(" TRAFIEŃ:\n");

                        Bitmap wynikowaBitmapa = croppedAndScaledBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(wynikowaBitmapa);

                        Paint paintKolo = new Paint();
                        paintKolo.setColor(Color.RED);
                        paintKolo.setStyle(Paint.Style.STROKE);
                        paintKolo.setStrokeWidth(12f);

                        Paint paintTekst = new Paint();
                        paintTekst.setColor(Color.GREEN);
                        paintTekst.setTextSize(60f);
                        paintTekst.setFakeBoldText(true);

                        // --- MATEMATYKA TARCZY ---
                        // Zmieniaj SKALA_TARCZY (np. 0.82f, 0.85f), by dostroić granice punktów
                        float SKALA_TARCZY = 0.84f;
                        float srodekX = 1024f;
                        float srodekY = 1024f;
                        double promienTarczy = 1024.0 * SKALA_TARCZY;
                        double szerokoscStrefy = promienTarczy / 10.0;

                        for (int i = 0; i < wyniki.length; i++) {
                            Detection detekcja = wyniki[i];
                            float x = detekcja.x;
                            float y = detekcja.y;

                            // TWARDE WYLICZENIE PUNKTÓW Z GEOMETRII
                            double odlegloscOdSrodka = Math.sqrt(Math.pow(x - srodekX, 2) + Math.pow(y - srodekY, 2));
                            int wyliczonePunkty = 0;

                            if (odlegloscOdSrodka <= promienTarczy) {
                                int strefa = (int) (odlegloscOdSrodka / szerokoscStrefy);
                                wyliczonePunkty = 10 - strefa;
                            }
                            if (wyliczonePunkty < 0) wyliczonePunkty = 0;

                            // Malowanie kółka i wyliczonego punktu
                            canvas.drawCircle(x, y, 40f, paintKolo);
                            canvas.drawText(String.valueOf(wyliczonePunkty), x + 45, y + 20, paintTekst);

                            raport.append(String.format(Locale.US, "[%d] Punkty: %d\n", i+1, wyliczonePunkty));
                        }

                        runOnUiThread(() -> {
                            txtResult.setText(raport.toString());
                            imageViewResult.setImageBitmap(wynikowaBitmapa);
                            imageViewResult.setVisibility(View.VISIBLE);
                            imageViewResult.setOnClickListener(v -> v.setVisibility(View.GONE));
                        });

                    } else {
                        runOnUiThread(() -> txtResult.setText("Nie znalazłem przestrzelin."));
                    }

                    // Optymalizacja pamięci
                    rawBitmap.recycle();
                    rotatedBitmap.recycle();
                    squareBitmap.recycle();

                } catch (Exception e) {
                    Log.e("ImageProcess", "Błąd przy obróbce: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błąd obróbki obrazu!", Toast.LENGTH_SHORT).show());
                } finally {
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Błąd robienia zdjęcia: " + exception.getMessage());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Aparat nie złapał klatki", Toast.LENGTH_SHORT).show());
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
        permissionHelper.handlePermissionsResult(requestCode, grantResults);
    }
}