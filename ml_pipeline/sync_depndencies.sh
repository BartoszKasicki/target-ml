#!/bin/bash

# Zatrzymanie skryptu w przypadku jakiegokolwiek błędu
set -e

echo "🔄 Rozpoczynam synchronizację środowiska ML..."

# 1. Sprawdzenie, czy istnieje folder venv. Jeśli nie - tworzy go.
if [ ! -d "venv" ]; then
    echo "📦 Nie znaleziono folderu venv. Tworzę nowe środowisko (Python 3.11)..."
    # Używamy konkretnie python3.11, bo wiemy, że działa!
    python3.11 -m venv venv
else
    echo "✅ Znaleziono istniejące środowisko venv."
fi

# 2. Aktywacja środowiska
echo "🔌 Aktywacja środowiska..."
source venv/bin/activate

# 3. Aktualizacja menedżera pakietów
echo "⬆️ Aktualizacja narzędzia pip..."
pip install --upgrade pip --quiet

# 4. Instalacja zależności
echo "📥 Instalowanie pakietów z requirements.txt (to może chwilę potrwać)..."
pip install -r requirements.txt

echo "========================================"
echo "🎉 Gotowe! Środowisko zsynchronizowane."
echo "👉 Aby je ręcznie aktywować w terminalu, wpisz: source venv/bin/activate"
echo "========================================"