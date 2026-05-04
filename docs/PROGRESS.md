# Raport Postępu Prac - Projekt: Detekcja Przestrzelin na Tarczy

## 1. Opis Projektu
Projekt zakłada stworzenie kompleksowego systemu do automatycznego wykrywania i analizy przestrzelin na tarczy strzeleckiej. Rozwiązanie opiera się na architekturze Edge AI (obliczenia na urządzeniu) i składa się z trzech głównych filarów technologicznych:
1. **Generowanie Danych:** Tworzenie syntetycznego zbioru danych uczących z wykorzystaniem silnika renderującego (OpenGL/C++) oraz generowanie adnotacji w formacie YOLO.
2. **Moduł Machine Learning:** Trening sieci neuronowej do detekcji obiektów w oparciu o architekturę **YOLO** (np. YOLOv8/YOLOv11 w środowisku PyTorch), zakończony konwersją modelu (.onnx -> .param/.bin).
3. **Aplikacja Mobilna (Android):** Aplikacja natywna (Java/Kotlin, wzorzec MVVM) przechwytująca obraz na żywo (CameraX), która przy pomocy interfejsu JNI oraz C++ realizuje wnioskowanie za pomocą lekkiego frameworka NCNN i biblioteki OpenCV.

---

## 2. Podział ról w zespole i wykorzystywane technologie
*   **Prowadzący (Generator Danych):** Budowa generatora w OpenGL/C++, renderowanie tarcz ze zmiennym oświetleniem/perspektywą oraz eksport adnotacji bounding boxów (format YOLO).
*   **Student 1 (Inżynier ML- Student 1):** Przygotowanie środowiska w Pythonie (PyTorch), trening na danych syntetycznych, ewaluacja modelu YOLO oraz jego konwersja do formatu NCNN. Współpraca przy logice wnioskowania w C++.
*   **Student 2 (Inżynier Mobile - Student 2):** Implementacja UI/UX, zarządzanie cyklem życia aplikacji i sprzętem (CameraX), natywny pre-processing obrazu, optymalizacja pamięci operacyjnej (RAM). Współpraca przy integracji mostu JNI.
*   **Zadanie Wspólne (Integracja Natywna):** Stworzenie mostu JNI łączącego kod Javy z C++, implementacja logiki wczytywania wag NCNN oraz przesyłania klatek obrazu do klasyfikatora na urządzeniu mobilnym.

---

## 3. Stan prac - Model AI & NCNN (ZREALIZOWANE) (STUDENT 1)

- [ ] **Trening modelu:** - załadowano pierwszą paczkę danych testowych.
- [ ] **Struktura** - Przygotowano strukturę pod uczenie maszynowe. 
- [ ] **Python** - Przygotowano wewnętrzne środowisko python venv - jest w pliku {.gitignore} naleźy przygotować je lokalnie z pomocą instrukcji {docs/initial_setup.md}. 

---

## 4. Stan prac - Moduł Android & Przetwarzanie (ZREALIZOWANE)(STUDENT 2)

### Wykonane zadania:
- [x] **Obsługa Kamery:** Pełna integracja CameraX z obsługą wielu obiektywów i dynamicznym wyborem kamery.
- [x] **System Zoom:** Implementacja liniowego sterowania przybliżeniem (Slider) dla precyzyjnego celowania w tarczę.
- [x] **Zarządzanie Obrazem:** Autorski mechanizm przechwytywania klatek bezpośrednio do pamięci RAM (pominiecie wolnego zapisu na dysk).
- [x] **Pre-processing (OpenCV/Native):** 
    - Automatyczna korekta rotacji klatki z matrycy.
    - Implementacja precyzyjnego wycinania (Crop) środka obrazu (celownika) do formatu 224x224 px.
- [x] **Optymalizacja Pamięci:** Zastosowanie mechanizmu `.recycle()` dla bitmap, zapobiegające awariom aplikacji przy seryjnym robieniu zdjęć.
- [x] **Moduł Testowy:** System zapisu wyciętych próbek do folderu `Pictures/Przestrzeliny` w celu budowania bazy do testów modelu.

---


## 5. Następne kroki (Planowane)
1. **Integracja JNI/C++:** Przygotowanie mostu łączącego kod Javy z biblioteką NCNN.
2. **Implementacja wnioskowania:** Przekazanie wyciętej bitmapy do modelu i odebranie wyniku prawdopodobieństwa.
3. **Wyświetlanie wyników:** Prezentacja werdyktu AI w czasie rzeczywistym na ekranie (UI).
4. **Zapis wyników AI:** Dodanie informacji o pewności modelu do nazwy zapisywanych plików (np. `AI_98proc_success.jpg`).

---

## 6. Harmonogram i Kamienie Milowe (Milestones)
- **Faza 1 - Model AI (Student 1):** Trening sieci neuronowej, ewaluacja na danych syntetycznych i konwersja wag do lekkiego formatu NCNN (.param, .bin). **[W TRAKCIE]**
- **Faza 2 - Aplikacja bazowa (Student 2):** Architektura Android, integracja CameraX, pre-processing obrazu (natywnie) oraz system zapisu próbek testowych. **[ZREALIZOWANA]**
- **Faza 3 - Wdrożenie i Integracja (Wspólnie):** Wpięcie plików modelu do kodu C++ / JNI w aplikacji Android (szacowany nakład roboczy: ok. 10-12 godzin po zamknięciu Fazy 2). **[OCZEKUJĄCA]**
- **Faza 4 - Finalizacja (Student 2):** Kalibracja systemu pod kątem UX, dynamiczne wyświetlanie wyników w UI i testy wydajnościowe na fizycznym urządzeniu (szacowany nakład roboczy: ok. 4 godziny). **[PLANOWANA]**
