# Raport Postępu Prac - Projekt: Detekcja Przestrzelin na Tarczy

## 1. Opis Projektu
Aplikacja mobilna (Android) służąca do automatycznego wykrywania i analizy przestrzelin na tarczy strzeleckiej w czasie rzeczywistym. System wykorzystuje kamerę urządzenia, bibliotekę OpenCV do wstępnej obróbki obrazu oraz silnik NCNN do wnioskowania za pomocą sieci neuronowych.

---

## 2. Podział ról w zespole
*   **Programista Android (Student 2):** Architektura aplikacji, obsługa CameraX, optymalizacja obrazu (OpenCV), zarządzanie pamięcią, integracja modułu wnioskowania.
*   **Specjalista AI(Student 1):** Trening modelu, konwersja do formatu NCNN, optymalizacja wag sieciowych.
---
## 3. Stan prac - Model AI & NCNN (ZREALIZOWANE) (STUDENT 1)

> **Sekcja dla kolegi:** Proszę o wpisanie aktualnego stanu prac nad modelem.

- [ ] **Trening modelu:** (np. Etap zbierania danych / Trening wstępny / Ewaluacja)
- [ ] **Konwersja:** (np. ONNX -> NCNN)
- [ ] **Optymalizacja:** (np. Kwantyzacja modelu do FP16/INT8)
- [ ] **Pliki końcowe:** (Oczekiwanie na pliki .param i .bin)

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

## 6. Szacowany czas zakończenia
- Prace nad modułem Android: **Zakończone**.
- Integracja modelu (po otrzymaniu plików): **ok. 10-12h pracy**.
- Testy końcowe i poprawki UI: **4h pracy**.
