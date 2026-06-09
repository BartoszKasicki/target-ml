package com.example.przestrzeliny_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.FileOutputStream;
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

    private CameraManager cameraManager;
    private PermissionHelper permissionHelper;
    private ExecutorService cameraExecutor;
    private YoloDetector yoloDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()) Log.d("OPENCV", "OpenCV załadowane");

        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        zoomSlider = findViewById(R.id.zoomSlider);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        txtResult = findViewById(R.id.txtResult);
        imageViewResult = findViewById(R.id.imageViewResult);


        imageViewResult.setOnClickListener(v -> {

            imageViewResult.setVisibility(View.GONE);
        });

        yoloDetector = new YoloDetector();
        yoloDetector.initModel(getAssets(), "model.ncnn.param", "model.ncnn.bin");

        cameraManager = new CameraManager(this, viewFinder, zoomSlider);
        btnSwitchCamera.setOnClickListener(v -> cameraManager.switchCamera());

        permissionHelper = new PermissionHelper(this, new PermissionHelper.PermissionListener() {
            @Override public void onPermissionsGranted() { cameraManager.startCamera(); }
            @Override public void onPermissionsDenied() { finish(); }
        });

        if (permissionHelper.allPermissionsGranted()) cameraManager.startCamera();
        else permissionHelper.requestPermissions();

        btnCapture.setOnClickListener(v -> takePhotoAndProcess());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhotoAndProcess() {
        // Kliknięto przycisk  ->  teraz czyścimy stary wynik i piszemy, że analizujemy
        txtResult.setText("Analizuję strzał...");

        ImageCapture imageCapture = cameraManager.getImageCapture();
        if (imageCapture == null) {
            txtResult.setText("Czekam na strzał...");
            return;
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    Bitmap rawBitmap = imageProxyToBitmap(image);
                    int rotationDegrees = image.getImageInfo().getRotationDegrees();
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);

                    View viewFinder = findViewById(R.id.viewFinder);
                    View targetFrame = findViewById(R.id.targetFrame);
                    float scale = Math.max((float) viewFinder.getWidth() / rotatedBitmap.getWidth(), (float) viewFinder.getHeight() / rotatedBitmap.getHeight());
                    int cropSize = (int) (targetFrame.getWidth() / scale);
                    int startX = Math.max(0, (rotatedBitmap.getWidth() - cropSize) / 2);
                    int startY = Math.max(0, (rotatedBitmap.getHeight() - cropSize) / 2);

                    Bitmap squareBitmap = Bitmap.createBitmap(rotatedBitmap, startX, startY, cropSize, cropSize);
                    Bitmap croppedAndScaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 2048, 2048, true);

                    Detection[] wyniki = yoloDetector.processImage(croppedAndScaledBitmap);

                    if (wyniki != null && wyniki.length > 0) {
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

                        float SKALA_TARCZY = 0.84f;
                        float srodekX = 1024f, srodekY = 1024f;
                        double promienTarczy = 1024.0 * SKALA_TARCZY;
                        double szerokoscStrefy = promienTarczy / 10.0;

                        StringBuilder raport = new StringBuilder();
                        int sumaPunktow = 0; // Zmienna do liczenia sumy

                        for (Detection det : wyniki) {
                            double dystans = Math.sqrt(Math.pow(det.x - srodekX, 2) + Math.pow(det.y - srodekY, 2));
                            int punkty = Math.max(0, (int)(10 - (dystans / szerokoscStrefy)));

                            sumaPunktow += punkty; // Dodajemy trafienie do sumy całkowitej

                            canvas.drawCircle(det.x, det.y, 40f, paintKolo);
                            canvas.drawText(String.valueOf(punkty), det.x + 45, det.y + 20, paintTekst);
                            raport.append("Punkty: ").append(punkty).append("\n");
                        }

                        // Dodanie podsumowania na końcu raportu
                        raport.append("----------------\n");
                        raport.append("Suma: ").append(sumaPunktow);

                        runOnUiThread(() -> {
                            // Podmieniamy treść na raport z punktami i sumą
                            txtResult.setText(raport.toString());

                            imageViewResult.setImageBitmap(wynikowaBitmapa);
                            imageViewResult.setVisibility(View.VISIBLE);
                            saveResultToGallery(wynikowaBitmapa);
                        });
                    } else {
                        runOnUiThread(() -> {
                            txtResult.setText("Brak przestrzelin");
                            Toast.makeText(MainActivity.this, "AI nie wykryło przestrzelin.", Toast.LENGTH_SHORT).show();
                        });
                    }

                    rawBitmap.recycle();
                    rotatedBitmap.recycle();
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    runOnUiThread(() -> txtResult.setText("Błąd analizy"));
                } finally {
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("Error", exception.getMessage());
                runOnUiThread(() -> txtResult.setText("Czekam na strzał..."));
            }
        });
    }

    private void saveResultToGallery(Bitmap bitmap) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File dir = new File(path, "PrzestrzelinyApp");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "Wynik_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
            runOnUiThread(() -> Toast.makeText(this, "Zapisano wynik v Galerii!", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.handlePermissionsResult(requestCode, grantResults);
    }
}