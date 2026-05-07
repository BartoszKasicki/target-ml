# Instalacja projektu target_ml

## Moduł Machine Learning

Dokumentacja opisuje proces przygotowania środowiska dla modułu uczenia maszynowego (YOLOv11).

### Wymagane zależności
* **[Python 3.11](https://www.python.org/downloads/release/python-31115/)**
* **[YOLOv11 – Ultralytics](https://docs.ultralytics.com/models/yolo11/#performance-metrics)**


## Struktura Katalogów
```text
target-ml/
├── android-app/             # Kod aplikacji mobilnej
├── raw_data/                # Surowe zdjęcia i etykiety od prowadzącego
└── ml_pipeline/             # Skrypty uczenia maszynowego
    ├── requirements.txt     
    ├── sync_dependencies.sh 
    └── ...
```

### Instalacja i konfiguracja środowiska

1. **Zainstaluj Pythona:** Pobierz i zainstaluj język Python w wersji 3.14 z oficjalnej strony podanej wyżej.
2. **Przejdź do projektu:** Otwórz terminal i przejdź do głównego folderu projektu (np. `/Users/jmaciazek-mac/Documents/PI-UKSW/target-ml`).
3. **Utworzenie i aktywacja środowisko wirtualnego:** Wykonaj poniższe polecenie, aby stworzyć odizolowane środowisko o nazwie `venv`:

#### Automatyczna Instalacja (Zalecana)
Dla systemów **macOS / Linux**:
1. Wejdź do folderu: `cd ml_pipeline`
2. Nadaj uprawnienia: `chmod +x sync_dependencies.sh`
3. Uruchom: `./sync_dependencies.sh`
*(Skrypt sam utworzy wirtualne środowisko i pobierze stabilne, bezpieczne wersje bibliotek).*

#### Ręczna Instalacja (Windows / Wszystkie systemy)
Jeśli skrypt `.sh` nie działa (np. na Windowsie), wykonaj instalację ręcznie w terminalu:

**1. Stworzenie środowiska:**
*   **Windows:** `python -m venv venv`
*   **macOS / Linux:** `python3.11 -m venv venv`

**2. Aktywacja środowiska:**
*   **Windows (CMD):** `venv\Scripts\activate.bat`
*   **Windows (PowerShell):** `venv\Scripts\Activate.ps1`
*   **macOS / Linux:** `source venv/bin/activate`

**3. Instalacja zależności:**
```bash
pip install --upgrade pip
pip install -r requirements.txt --log install.log # Instalacja wymaganych bibliotek
```

#### Ręczna Instalacja OLD

```bash
python -m venv venv # Utworzenie 
source venv/bin/activate # Aktywacja
pip install --upgrade pip
cd ml_pipeline
pip install -r requirements.txt --log install.log # Instalacja wymaganych bibliotek
```

### Przygotowanie danych (Podział zbioru) - w przypadku pierwszej inicjalizacji z gotowej paczki danych zip
Aby model YOLO mógł się poprawnie uczyć, surowe dane muszą zostać podzielone na zbiór treningowy (`train`) i walidacyjny (`val`).

1. **Umieść dane źródłowe:** Upewnij się, że rozpakowane dane wejściowe znajdują się w folderze `dataset` (zawierającym podkatalogi `images` i `labels`).
2. **Uruchom skrypt podziału:** Będąc w głównym folderze projektu wykonaj poniższe polecenia, aby automatycznie wymieszać i rozdzielić pliki (domyślnie w proporcji 80/20):
```bash
cd ml_pipeline
python split_dataset.py
```
*Skrypt wygeneruje gotowy folder `.../target-ml/ml_pipeline/dataset`, z którego model będzie bezpośrednio korzystał podczas treningu.*

