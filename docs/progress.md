# Raport Postępu Prac - Projekt: Detekcja Przestrzelin na Tarczy

## 1. Opis Projektu
Projekt zakłada stworzenie kompleksowego systemu do automatycznego wykrywania i analizy przestrzelin na tarczy strzeleckiej. Rozwiązanie opiera się na architekturze Edge AI (obliczenia na urządzeniu) i składa się z trzech głównych filarów technologicznych:
1. **Generowanie Danych:** Tworzenie syntetycznego zbioru danych uczących z wykorzystaniem silnika renderującego (OpenGL/C++) oraz generowanie adnotacji w formacie YOLO.
2. **Moduł Machine Learning:** Trening sieci neuronowej do detekcji obiektów w oparciu o architekturę **YOLO** (np. YOLOv8/YOLOv11 w środowisku PyTorch), zakończony konwersją modelu (.onnx -> .param/.bin).
3. **Aplikacja Mobilna (Android):** Aplikacja natywna (Java/Kotlin, wzorzec MVVM) przechwytująca obraz na żywo (CameraX), która przy pomocy interfejsu JNI oraz C++ realizuje wnioskowanie za pomocą lekkiego frameworka NCNN i biblioteki OpenCV.

---

## 2. Podział ról w zespole i wykorzystywane technologie
* **Prowadzący (Generator Danych):** Budowa generatora w OpenGL/C++, renderowanie tarcz ze zmiennym oświetleniem/perspektywą oraz eksport adnotacji bounding boxów (format YOLO).
* **Student 1 (Inżynier ML):** Przygotowanie środowiska w Pythonie (PyTorch), trening na danych syntetycznych, ewaluacja modelu YOLO oraz jego konwersja do formatu NCNN. Współpraca przy logice wnioskowania w C++.
* **Student 2 (Inżynier Mobile):** Implementacja UI/UX, zarządzanie cyklem życia aplikacji i sprzętem (CameraX), natywny pre-processing obrazu, optymalizacja pamięci operacyjnej (RAM). Współpraca przy integracji mostu JNI.
* **Zadanie Wspólne (Integracja Natywna):** Stworzenie mostu JNI łączącego kod Javy z C++, implementacja logiki wczytywania wag NCNN oraz przesyłania klatek obrazu do klasyfikatora na urządzeniu mobilnym.

---

## 3. Stan prac - Model AI & NCNN (ZREALIZOWANE)
- [x] **Trening modelu** – Pomyślnie przeprowadzono trening modelu YOLOv11n na pierwszej paczce danych testowych. Skonfigurowano mechanizm Early Stopping, optymalizujący czas nauki.
- [x] **Infrastruktura danych** – Wdrożono potok przetwarzania danych (pipeline): od surowych obrazów w `raw_data/` po automatyczny podział na zbiory treningowe/walidacyjne w `ml_pipeline/dataset/`.
- [x] **Python** – Przygotowano wewnętrzne środowisko python venv (uwzględnione w `.gitignore`). Ustabilizowano środowisko oparte na Python 3.11 z rygorystycznym zamrożeniem wersji bibliotek `numpy` oraz `opencv-python`, co wyeliminowało konflikty z silnikiem PyTorch na systemach macOS.
- [x] **Naprawa etykiet** – Zaimplementowano skrypt `fix_labels.py` automatycznie korygujący indeksy klas w adnotacjach dostarczanych przez symulator (mapowanie ID 10 -> 0).
- [x] **Automatyzacja** – Przygotowano skrypt `sync_dependencies.sh` do błyskawicznej replikacji środowiska venv na innych stacjach roboczych.
- [x] **Most JNI/C++** – Ukończono implementację warstwy natywnej (`ncnn_bridge.cpp`), zapewniającą stabilną komunikację między Javą a frameworkkiem NCNN.
- [x] **System Obiektowy** – Wdrożono architekturę obiektową (klasy `Detection.java`, `YoloDetector.java`), pozwalającą na czytelne przekazywanie współrzędnych i pewności modelu z C++ do UI Androida.
- [x] **Wnioskowanie (Inference)** – Zaimplementowano proces przetwarzania klatek w czasie rzeczywistym. Model pracuje na pełnej rozdzielczości wejściowej modelu (2048x2048 px), co zapewnia wysoką precyzję detekcji małych obiektów.
- [x] **Geometria Punktacji** – Zaimplementowano silnik obliczeniowy w Javie, który w oparciu o geometrię odległości od środka (twarda matematyka) niezależnie od klasyfikacji modelu wylicza punktację (1-10) dla każdej wykrytej przestrzeliny.

---

## 4. Stan prac - Moduł Android & Przetwarzanie (ZREALIZOWANE)
- [x] **Obsługa Kamery** – Pełna integracja CameraX z obsługą wielu obiektywów i dynamicznym wyborem kamery.
- [x] **System Zoom** – Implementacja liniowego sterowania przybliżeniem (Slider) dla precyzyjnego celowania w tarczę.
- [x] **Zarządzanie Obrazem** – Autorski mechanizm przechwytywania klatek bezpośrednio do pamięci RAM (pominięcie wolnego zapisu na dysk przed przetwarzaniem).
- [x] **Pre-processing (OpenCV/Native)** –
  - Automatyczna korekta rotacji klatki bezpośrednio z matrycy aparatu.
  - Implementacja precyzyjnego wycinania (Crop) środka obrazu (obszaru tarczy) i skalowania do formatu wejściowego modelu (2048x2048 px).
- [x] **Optymalizacja Pamięci** – Zastosowanie mechanizmu `.recycle()` dla bitmap oraz poprawne zamykanie zasobów `ImageProxy`, co skutecznie zapobiega wyciekom pamięci (OutOfMemoryError) przy seryjnym robieniu zdjęć.
- [x] **Wizualizacja wyników** – Zintegrowano warstwę graficzną (`Canvas`), która dynamicznie nanosi wykryte przestrzeliny (czerwone okręgi) oraz ich wartości punktowe (zielony tekst) bezpośrednio na podgląd zdjęcia.
- [x] **Integracja Galerii** – Wdrożono mechanizm `MediaScannerConnection`, umożliwiający automatyczny zapis przetworzonych zdjęć z nałożonymi wynikami do systemowej galerii w dedykowanym folderze publicznym `Pictures/PrzestrzelinyApp`.
- [x] **Obsługa Uprawnień** – Zaimplementowano dynamiczny system uprawnień (Runtime Permissions) zgodny z polityką nowoczesnych wersji systemu Android (Scoped Storage).

---

## 5. Podsumowanie i Wnioski
Projekt osiągnął założony cel główny – stworzenie w pełni funkcjonalnego systemu detekcji przestrzelin działającego lokalnie na urządzeniu mobilnym (Edge AI). 

**Główne wnioski techniczne:**
1. Zbudowano kompletny i stabilny potok przetwarzania danych: od pobrania surowego obrazu z kamery, przez natywną warstwę wnioskowania AI, aż po matematyczną weryfikację i wizualizację wyników.
2. Zidentyfikowano różnice w zachowaniu modelu między środowiskiem testowym (Python/PyTorch na PC) a silnikiem mobilnym (NCNN). Model wykazuje znakomitą skuteczność w czarnym polu tarczy, jednak pre-processing NCNN wpływa na spadek czułości na białym tle.
3. **Rekomendacja na przyszłość:** Kolejnym krokiem rozwojowym powinno być dotrenowanie modelu (fine-tuning) z wykorzystaniem dedykowanego zbioru zdjęć wykonanych bezpośrednio aparatami mobilnymi (z uwzględnieniem zaszumienia matrycy i specyfiki kompresji mobilnej) lub przeniesienie inferencji na zewnętrzne API sieciowe w celu odciążenia urządzenia.

---

## 6. Harmonogram i Kamienie Milowe (Milestones)
- **Faza 1 - Model AI (Student 1):** Trening sieci neuronowej, ewaluacja na danych syntetycznych i konwersja wag do lekkiego formatu NCNN (`.param`, `.bin`). **[ZREALIZOWANA]**
- **Faza 2 - Aplikacja bazowa (Student 2):** Architektura Android, integracja CameraX, pre-processing obrazu (natywnie) oraz system zapisu próbek testowych. **[ZREALIZOWANA]**
- **Faza 3 - Wdrożenie i Integracja (Wspólnie):** Wpięcie plików modelu do kodu C++ / JNI w aplikacji Android, mapowanie struktur danych dla klasy `Detection`. **[ZREALIZOWANA]**
- **Faza 4 - Finalizacja (Student 2):** Kalibracja systemu pod kątem UX, implementacja geometrii punktacji, dynamiczne wyświetlanie wyników w UI, zapis gotowych raportów graficznych do galerii urządzenia oraz testy wydajnościowe na fizycznym smartfonie. **[ZREALIZOWANA]**
