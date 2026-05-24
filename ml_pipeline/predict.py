import os
from ultralytics import YOLO

def main():
    # 1. Ścieżka do najlepszego modelu z ostatniego treningu
    model_path = '../runs/detect/target_runs/yolo11_holes_v1-4/weights/best.pt'
    
    if not os.path.exists(model_path):
        print(f"❌ Nie znaleziono modelu: {model_path}")
        return

    model = YOLO(model_path)

    # 2. Ścieżka do zdjęcia testowego 
    image_to_test = '../raw_data/images/0001.png' 

    print(f"🔍 Uruchamiam detekcję przestrzelin na zdjęciu: {image_to_test}")
    
    # 3. Predykcja
    # conf=0.25 - model pokaże tylko te ramki, co do których jest pewny na min. 25%
    # save=True - zapisze nowe zdjęcie z narysowanymi ramkami
    # show=True - wyświetli okienko ze zdjęciem na Twoim Macu
    results = model.predict(source=image_to_test, conf=0.25, save=True, show=True)
    
    print("\n✅ Gotowe! Jeśli predykcja się powiodła, zdjęcie z ramkami zostało zapisane.")
    print("Sprawdź folder: ../runs/detect/predict/")

if __name__ == '__main__':
    main()