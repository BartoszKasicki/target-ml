from ultralytics import YOLO

def main():
    # Pobranie pre-trenowanego modelu
    model = YOLO('yolo11n.pt') 

    print("🚀 Rozpoczynam trening modelu na procesorze głównym (Intel CPU)...")
    print("⏳ Trening na CPU może potrwać dłużej. Warto zostawić komputer podłączony do prądu.")
    
    results = model.train(
        data='dataset.yaml',   
        epochs=50,             
        imgsz=640,             
        batch=16,              
        patience=15,           
        device='cpu',
        project='target_runs', 
        name='yolo11_holes_v1' 
    )
    
    print("✅ Trening zakończony pomyślnie!")

if __name__ == '__main__':
    main()