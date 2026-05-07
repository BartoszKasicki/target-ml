import os
import random
import shutil
from pathlib import Path

def split_raw_data(source_dir, output_dir, split_ratio=0.8):

    src_images = Path(source_dir) / 'images'
    src_labels = Path(source_dir) / 'labels' 

    out_dir = Path(output_dir)
    
    dirs_to_make = [
        out_dir / 'images' / 'train', out_dir / 'images' / 'val',
        out_dir / 'labels' / 'train', out_dir / 'labels' / 'val'
    ]
    
    # Tworzenie struktury folderów
    for d in dirs_to_make:
        d.mkdir(parents=True, exist_ok=True)
        
    # Pobieranie listy obrazków
    images = [f for f in os.listdir(src_images) if f.endswith(('.png', '.jpg', '.jpeg'))]
    random.shuffle(images) # Mieszamy, żeby model nie uczył się po kolei
    
    # Obliczanie podziału (np. 80% train, 20% val)
    train_size = int(len(images) * split_ratio)
    train_images = images[:train_size]
    val_images = images[train_size:]
    
    def copy_files(file_list, split_type):
        for img_name in file_list:
            # Kopiowanie obrazka
            shutil.copy(src_images / img_name, out_dir / 'images' / split_type / img_name)
            
            # Kopiowanie etykiety (zmiana rozszerzenia z np. .png na .txt)
            label_name = os.path.splitext(img_name)[0] + '.txt'
            if (src_labels / label_name).exists():
                shutil.copy(src_labels / label_name, out_dir / 'labels' / split_type / label_name)
            else:
                print(f"⚠️ UWAGA: Brak pliku etykiety dla {img_name}")

    print(f"📦 Rozpoczynam podział: {len(train_images)} do treningu, {len(val_images)} do walidacji...")
    copy_files(train_images, 'train')
    copy_files(val_images, 'val')
    print("✅ Gotowe! Utworzono folder dataset.")

if __name__ == '__main__':
    split_raw_data(source_dir='../raw_data', output_dir='dataset', split_ratio=0.8)
    