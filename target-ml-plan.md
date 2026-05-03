# Przestrzeliny - Plan Projektu

## 1. Mapowanie Technologii na Elementy Systemu
Aby zachować porządek, musimy dokładnie określić, gdzie i dlaczego użyjemy konkretnych narzędzi z Twojej listy:

* **Prowadzący: Generator Danych Syntetycznych**
    * **OpenGL / C++:** Do wydajnego renderowania tarcz strzeleckich (tekstury, oświetlenie, perspektywa) i "wycinania" otworów (przestrzelin).
    * **OpenCV (C++ lub Python):** Do zapisywania wyrenderowanych klatek jako obrazów (.jpg/.png) oraz weryfikacji wygenerowanych bounding boxów.
    * *Wynik prac:* Zbiór zdjęć (np. 10 000+ sztuk) oraz pliki tekstowe z adnotacjami w formacie YOLO (klasa, x_center, y_center, width, height).

* **Student 1: Środowisko ML i Model (Inżynier Machine Learning)**
    * **Python:** Główny język do skryptów treningowych i konwersji.
    * **PyTorch (zalecany) / TensorFlow:** Do trenowania sieci detekcji obiektów. Ze względu na specyfikę projektu, idealnie sprawdzi się architektura YOLO (np. YOLOv8 lub YOLOv11 z biblioteki Ultralytics), która natywnie korzysta z PyTorcha.
    * **Format ONNX & NCNN:** Skrypty konwertujące wytrenowany model `.pt` do formatu `.onnx`, a następnie do lekkiego `.param` i `.bin` wymaganego przez NCNN na urządzenia mobilne.

* **Student 2: Aplikacja Android (Inżynier Mobile)**
    * **Kotlin (zamiast Javy - standard rynkowy):** Do logiki UI, zarządzania stanem aplikacji i obsługi uprawnień.
    * **Android SDK & CameraX:** Do przechwytywania strumienia wideo z kamery w czasie rzeczywistym i wyciągania pojedynczych klatek.

* **Student 1 + Student 2: Integracja Natywna (Najtrudniejszy punkt projektu)**
    * **C++ & NCNN (Framework):** Logika wczytywania modelu NCNN i inferencji na urządzeniu mobilnym.
    * **OpenCV (C++ dla Androida):** Przetwarzanie klatki z CameraX (zmiana formatu YUV na RGB, skalowanie, normalizacja) przed podaniem jej do modelu NCNN.
    * **JNI (Java Native Interface):** Most łączący kod napisany w Kotlinie z kodem w C++.

---

## 2. Proponowany Plan Wykonania (Fazy Projektu)

### Faza 1: Ustalenie formatów i kontraktów (Tydzień 1) - Cały zespół
Zanim napiszecie linijkę kodu, musicie ustalić, jak poszczególne moduły będą się ze sobą komunikować.
* Prowadzący i Student 1 ustalają format adnotacji (np. standard YOLO: `class x y w h`).
* Studenci 1 i 2 ustalają, jak model C++ będzie zwracał dane do Kotlina (np. tablica obiektów: `[id_klasy, x, y, szerokość, wysokość, pewność_sieci]`).

### Faza 2: Generowanie danych (Tygodnie 2-4) - Prowadzący
* Stworzenie silnika renderującego tarcze w OpenGL.
* Implementacja logiki losowego rozmieszczania przestrzelin, zmiennego oświetlenia, obrotu tarczy (dane muszą być zróżnicowane, tzw. Data Augmentation u źródła).
* Eksport gotowego datasetu i udostępnienie go (np. przez Google Drive, DVC).

### Faza 3: Trening Modelu ML (Tygodnie 3-6) - Student 1
* Wczytanie syntetycznych danych.
* Konfiguracja środowiska w Pythonie (PyTorch, YOLO).
* Trening modelu i ewaluacja (wykresy mAP, Loss).
* Testowanie modelu na kilku prawdziwych zdjęciach tarcz w celu weryfikacji.
* Konwersja modelu: PyTorch -> ONNX -> NCNN.

### Faza 4: Szkielet Aplikacji Android (Tygodnie 3-6) - Student 2
* Przygotowanie projektu w Android Studio (C++ / Native support włączony).
* Implementacja CameraX (podgląd na żywo).
* Dodanie przycisku "Przelicz" i zablokowanie klatki (ImageProxy).
* Stworzenie interfejsu (np. Canvas) do rysowania bounding boxów na podglądzie.

### Faza 5: Integracja JNI i NCNN (Tygodnie 7-9) - Student 1 i 2 (Współpraca)
* Podpięcie bibliotek NCNN i pre-kompilowanego OpenCV for Android do pliku `CMakeLists.txt`.
* Napisanie mostu JNI: Kotlin przesyła klatkę obrazu -> JNI -> C++ (OpenCV formatuje obraz) -> NCNN robi predykcję -> C++ zwraca wyniki -> JNI -> Kotlin rysuje wynik.
* Implementacja algorytmu obliczającego ostateczną punktację (na podstawie pozycji detekcji względem środka tarczy). Algorytm ten można napisać w Kotlinie na podstawie zwróconych współrzędnych lub w C++.

### Faza 6: Testowanie i Optymalizacja (Tygodnie 10-12) - Cały zespół
* Testy na rzeczywistych strzelnicach (bardzo ważne!).
* Analiza wydajności (czy aplikacja nie "przycina" przy wciśnięciu przycisku).
* Ewentualne dotrenowanie modelu na poprawionym zbiorze danych od Prowadzącego.

---

## 3. Proponowana Struktura Repozytorium (Monorepo)
Aby ułatwić współpracę, zalecam trzymanie wszystkiego w jednym repozytorium Git, podzielonym na wyraźne katalogi:

```text
TargetScoringProject/
├── data_generator/         # Projekt Prowadzącego (C++, OpenGL)
│   ├── src/
│   ├── CMakeLists.txt
│   └── output/             # (Ignorowany w git) Wygenerowane dataset'y
├── ml_pipeline/            # Kod Studenta 1 (Python)
│   ├── requirements.txt
│   ├── train.py
│   ├── evaluate.py
│   └── export_ncnn.py      # Skrypty do konwersji modeli
└── android_app/            # Projekt Studenta 2 i 1 (Kotlin/Java/C++)
    ├── app/
    │   ├── src/main/java/  # Kod UI, CameraX, wywołania JNI
    │   ├── src/main/cpp/   # C++, pliki nagłówkowe NCNN, logika inferencji
    │   └── src/main/res/   # Layouty XML / Jetpack Compose
    ├── CMakeLists.txt      # Konfiguracja kompilacji kodu natywnego (NDK)
    └── models/             # Gotowe pliki .param i .bin modelu NCNN
```

---

## 4. Najlepsze Praktyki i Wzorce (Do zastosowania)

### Architektura i Mobile (Student 2)
* **MVVM (Model-View-ViewModel):** Wzorzec obowiązkowy dla współczesnych aplikacji Android. Pozwoli oddzielić logikę obsługi kamery (View) od stanu aplikacji i wyników punktacji (ViewModel).
* **Jetpack Compose:** Zamiast starych widoków XML, warto rozważyć Compose do stworzenia UI – przyspieszy to rysowanie wyników (nakładki na kamerę).
* **Coroutine (Kotlin):** Obsługa modelu NCNN poprzez JNI zablokuje wątek. Należy użyć Kotlin Coroutines (np. `Dispatchers.Default`), aby przenieść wyliczenia w C++ z wątku głównego (UI Thread) i uniknąć błędu ANR (Application Not Responding).

### Uczenie Maszynowe i C++ (Student 1 i Prowadzący)
* **RAII (Resource Acquisition Is Initialization):** W kodzie C++ na Androidzie kluczowe jest zarządzanie pamięcią. Należy uważać na wycieki przy konwersji obrazów między Java a C++ (zwalnianie buforów w JNI).
* **Transfer Learning:** Nie trenujcie modelu od zera. Użyjcie pre-trenowanego modelu (np. YOLOv8 Nano - `yolov8n.pt`), który już rozumie kształty, i zróbcie fine-tuning na waszym syntetycznym datasecie przestrzelin. Model "Nano" jest kluczowy dla płynnego działania na smartfonach przez NCNN.
* **Wzorzec Singleton dla Modelu w C++:** Ładowanie wag modelu NCNN (plików `.bin` i `.param`) do pamięci RAM telefonu trwa chwilę i zużywa zasoby. Należy to zrobić tylko raz (podczas startu aplikacji), trzymać instancję w C++ i używać jej do kolejnych predykcji.
* **Domain Randomization (Dla Prowadzącego):** Generując dane w OpenGL, zmieniajcie wszystko: kąt padania światła, cienie, perspektywę, kolory tarcz, "brud", jakość renderu. Sieci neuronowe są leniwe – jeśli każda syntetyczna tarcza będzie idealnie równa, model nie zadziała na prawdziwym, poruszonym zdjęciu z telefonu.
