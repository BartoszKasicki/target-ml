package com.example.przestrzeliny_app;

public class Detection {
    public float x;
    public float y;
    public float width;
    public float height;
    public int label;       // Punktacja (np. 10, 9, 8...)
    public float confidence; // Pewność modelu (np. 0.88)

    // Konstruktor, którego będzie używać C++ do tworzenia tego obiektu
    public Detection(float x, float y, float width, float height, int label, float confidence) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.confidence = confidence;
    }
}