import os

def apply_patch():
    file_path = "app/src/main/java/com/lagradost/cloudstream3/ui/result/ResultFragmentTv.kt"
    
    if not os.path.exists(file_path):
        print(f"HATA: {file_path} bulunamadı!")
        return

    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    new_lines = []
    found = False
    
    # Orijinal kod bloğunu satır satır kontrol eder
    i = 0
    while i < len(lines):
        line = lines[i]
        # Aradığımız başlangıç noktası
        if "resultCastItems.adapter = ActorAdaptor(aboveCast?.id) {" in line:
            # Mevcut 3 satırlık bloğu atla ve yerine yenisini koy
            new_lines.append("            resultCastItems.adapter = ActorAdaptor(aboveCast?.id) { view ->\n")
            new_lines.append("                toggleEpisodes(false)\n")
            new_lines.append("                val actorName = (view?.tag as? com.lagradost.cloudstream3.ActorData)?.actor?.name\n")
            new_lines.append("                if (!actorName.isNullOrBlank()) {\n")
            new_lines.append("                    com.lagradost.cloudstream3.ui.quicksearch.QuickSearchFragment.pushSearch(activity, actorName)\n")
            new_lines.append("                }\n")
            new_lines.append("            }\n")
            
            # Orijinal bloğun bitişini (kapanış parantezini) bulana kadar satırları atla
            while i < len(lines) and "}" not in lines[i]:
                i += 1
            found = True
        else:
            new_lines.append(line)
        i += 1

    if found:
        with open(file_path, "w", encoding="utf-8") as f:
            f.writelines(new_lines)
        print("YAMA: Actor arama özelliği başarıyla eklendi.")
    else:
        print("HATA: Değiştirilecek kod bloğu bulunamadı. Orijinal dosya değişmiş olabilir.")

if __name__ == "__main__":
    apply_patch()