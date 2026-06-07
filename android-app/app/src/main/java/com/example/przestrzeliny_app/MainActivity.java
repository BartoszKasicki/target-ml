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

    //ładowanie biblioteki
    static {
        System.loadLibrary("ncnn_bridge");
    }

    //deklaracja "mostu"
    public native float[] detectBulletHoles(Bitmap bitmap, android.content.res.AssetManager assetManager);

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
        txtResult = findViewById(R.id.txtResult); //wyniki YOLO w przyszłości

        // Inicjalizacja Menedżera Kamery
        cameraManager = new CameraManager(this, viewFinder, zoomSlider);

        // Obsługa zmiany obiektywu
        btnSwitchCamera.setOnClickListener(v -> {
            cameraManager.switchCamera();
            Toast.makeText(this, "Szukam następnego obiektywu...", Toast.LENGTH_SHORT).show();
        });

        //  Inicjalizacja Pomocnika od uprawnień z "Krótkofalówką"
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

        //Sprawdzamy uprawnienia przy starcie
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
        // Pobieramy interfejs kamery
        androidx.camera.core.ImageCapture currentImageCapture = cameraManager.getImageCapture();
        if (currentImageCapture == null) return;

        currentImageCapture.takePicture(androidx.core.content.ContextCompat.getMainExecutor(this), new androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@androidx.annotation.NonNull androidx.camera.core.ImageProxy image) {
                try {
                    //Konwersja i obrót
                    android.graphics.Bitmap rawBitmap = imageProxyToBitmap(image);
                    int rotationDegrees = image.getImageInfo().getRotationDegrees();

                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    matrix.postRotate(rotationDegrees);
                    android.graphics.Bitmap rotatedBitmap = android.graphics.Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);

                    // IDEALNY CROP (MATEMATYKA CAMERAX) ---
                    android.view.View viewFinder = findViewById(R.id.viewFinder);
                    android.view.View targetFrame = findViewById(R.id.targetFrame);

                    int w_img = rotatedBitmap.getWidth();
                    int h_img = rotatedBitmap.getHeight();
                    int w_view = viewFinder.getWidth();
                    int h_view = viewFinder.getHeight();

                    // Obliczamy rzeczywistą skalę, jakiej Android użył do wypełnienia ekranu (FILL_CENTER)
                    float scale = Math.max((float) w_view / w_img, (float) h_view / h_img);

                    // Przeliczamy fizyczną wielkość ramki z ekranu na piksele matrycy aparatu
                    int cropSize = (int) (targetFrame.getWidth() / scale);

                    // Wyliczamy środek cięcia
                    int startX = (w_img - cropSize) / 2;
                    int startY = (h_img - cropSize) / 2;

                    // Zabezpieczenie na wypadek ułamków pikseli i wyjścia poza krawędź
                    startX = Math.max(0, startX);
                    startY = Math.max(0, startY);
                    if (startX + cropSize > w_img) cropSize = w_img - startX;
                    if (startY + cropSize > h_img) cropSize = h_img - startY;

                    // Cięcie! Zostaje 100% to, co było widać w ramce
                    android.graphics.Bitmap squareBitmap = android.graphics.Bitmap.createBitmap(rotatedBitmap, startX, startY, cropSize, cropSize);

                    // SKALOWANIE DLA AI
                    // Skalujemy wyciętą tarczę dla naszej sztucznej inteligencji (wymuszone 2048x2048)
                    int targetSize = 2048;
                    android.graphics.Bitmap croppedAndScaledBitmap = android.graphics.Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true);

                    // Zapisujemy ten wycięty ideał do galerii, żeby widzieć co poszło do AI
                    saveBitmapToGallery(croppedAndScaledBitmap);

                    // PRZESYŁAMY DO AI (Most C++)
                    float[] wyniki = detectBulletHoles(croppedAndScaledBitmap, getAssets());

                    //odbieramy to co widzi AI
                    if (wyniki != null && wyniki.length > 0) {
                        int liczbaPrzestrzelin = wyniki.length / 4;
                        StringBuilder raport = new StringBuilder();
                        raport.append("ZNALAZŁEM ").append(liczbaPrzestrzelin).append(" STRZAŁ(ÓW):\n");
                        raport.append("=== SUROWE DANE Z NCNN ===\n");

                        // Przygotowujemy pędzle i nową bitmapę do narysowania czerwonych celowników
                        android.graphics.Bitmap wynikowaBitmapa = croppedAndScaledBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
                        android.graphics.Canvas canvas = new android.graphics.Canvas(wynikowaBitmapa);

                        android.graphics.Paint paintKolo = new android.graphics.Paint();
                        paintKolo.setColor(android.graphics.Color.RED);
                        paintKolo.setStyle(android.graphics.Paint.Style.STROKE);
                        paintKolo.setStrokeWidth(15f); // Grubość celownika

                        android.graphics.Paint paintTekst = new android.graphics.Paint();
                        paintTekst.setColor(android.graphics.Color.GREEN);
                        paintTekst.setTextSize(60f); // Wielkość tekstu (punktów)
                        paintTekst.setFakeBoldText(true);

                        for (int i = 0; i < liczbaPrzestrzelin; i++) {
                            int baseIndex = i * 4;
                            float x = wyniki[baseIndex];
                            float y = wyniki[baseIndex + 1];
                            int klasaPunktow = (int) wyniki[baseIndex + 2];
                            float pewnosc = wyniki[baseIndex + 3] * 100;

                            // Malujemy kółko i podpis na zdjęciu
                            canvas.drawCircle(x, y, 40f, paintKolo);
                            canvas.drawText(String.valueOf(klasaPunktow), x + 50, y + 20, paintTekst);

                            // Formatowanie tekstu punktów
                            String wynikSlowny = klasaPunktow == 0 ? "0 punktów" : klasaPunktow == 1 ? "1 punkt" : klasaPunktow + " punkty";
                            if(klasaPunktow >= 5) wynikSlowny = klasaPunktow + " punktów";

                            raport.append(String.format(java.util.Locale.US, "[%d] Pkt: %d | Pewność: %.1f%% | Poz: X:%.0f, Y:%.0f\n", i+1, klasaPunktow, pewnosc, x, y));
                        }

                        // WYŚWIETLAMY WYNIKI W INTERFEJSIE UŻYTKOWNIKA
                        runOnUiThread(() -> {
                            // Pokazujemy tekst na ekranie
                            txtResult.setText(raport.toString());

                            // Wyciągamy wielki podgląd i rzucamy na niego zamrożone, pomalowane zdjęcie
                            android.widget.ImageView imageViewResult = findViewById(R.id.imageViewResult);
                            imageViewResult.setImageBitmap(wynikowaBitmapa);
                            imageViewResult.setVisibility(android.view.View.VISIBLE);

                            // Kiedy prowadzacy kliknie palcem w wynikowe zdjęcie, zniknie ono i będzie mógł zrobić następne
                            imageViewResult.setOnClickListener(v -> v.setVisibility(android.view.View.GONE));
                        });

                    } else {
                        runOnUiThread(() -> txtResult.setText("Nie znalazłem przestrzelin."));
                    }

                    //OPTYMALIZACJA - sprzątamy pamięć RAM telefonu po ciężkich operacjach
                    rawBitmap.recycle();
                    rotatedBitmap.recycle();
                    squareBitmap.recycle();

                } catch (Exception e) {
                    android.util.Log.e("ImageProcess", "Błąd przy obróbce: " + e.getMessage());
                    runOnUiThread(() -> android.widget.Toast.makeText(MainActivity.this, "Błąd obróbki obrazu!", android.widget.Toast.LENGTH_SHORT).show());
                } finally {
                    // Zamknięcie bufora aparatu - niezbędne, by móc zrobić kolejne zdjęcie
                    image.close();
                }
            }

            @Override
            public void onError(@androidx.annotation.NonNull androidx.camera.core.ImageCaptureException exception) {
                android.util.Log.e("CameraX", "Błąd robienia zdjęcia: " + exception.getMessage());
                runOnUiThread(() -> android.widget.Toast.makeText(MainActivity.this, "Aparat nie złapał klatki", android.widget.Toast.LENGTH_SHORT).show());
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
