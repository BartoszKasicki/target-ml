package com.example.przestrzeliny_app;


import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class YoloDetector {

    // Ładowanie biblioteki C++
    static {
        System.loadLibrary("ncnn_bridge");
    }

    // Inicjalizacja modelu
    public native boolean initModel(AssetManager mgr, String paramPath, String binPath);

    // Główna funkcja przetwarzająca zdjęcie
    public native Detection[] processImage(Bitmap bitmap);
}
