from pathlib import Path

def main():
    labels_dir = Path('dataset/labels')
    txt_files = list(labels_dir.rglob('*.txt'))
    
    if not txt_files:
        print("❌ Nie znaleziono plików .txt. Upewnij się, że folder 'dataset/labels' istnieje.")
        return

    fixed_count = 0
    for txt_file in txt_files:
        with open(txt_file, 'r') as f:
            lines = f.readlines()
        
        new_lines = []
        for line in lines:
            parts = line.strip().split()
            if len(parts) >= 5: # Format YOLO to 5 liczb: klasa x y w h
                parts[0] = '0'  # Wymuszamy klasę '0'
                new_lines.append(' '.join(parts) + '\n')
        
        with open(txt_file, 'w') as f:
            f.writelines(new_lines)
            
        fixed_count += 1
        
    print(f"✅ Sukces! Podmieniono ID klasy na '0' w {fixed_count} plikach adnotacji.")

if __name__ == '__main__':
    main()