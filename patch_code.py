import os

def apply_patch():
    file_path = "app/src/main/java/com/lagradost/cloudstream3/ui/result/ResultFragmentTv.kt"
    
    if not os.path.exists(file_path):
        print(f"HATA: {file_path} bulunamadı!")
        return

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Aradığımız anahtar kelimeyi içeren ama boşluklara duyarlı olmayan kontrol
    search_text = "resultCastItems.adapter = ActorAdaptor(aboveCast?.id) {"
    
    if search_text in content:
        # Eski bloğu bulup tamamen yenisiyle değiştiriyoruz
        # Not: Orijinal koddaki kapanış parantezine kadar olan kısmı hedefliyoruz
        old_block_start = content.find(search_text)
        old_block_end = content.find("}", old_block_start) + 1
        
        new_code = """resultCastItems.adapter = ActorAdaptor(aboveCast?.id) { view ->
                toggleEpisodes(false)
                val actorName = (view?.tag as? com.lagradost.cloudstream3.ActorData)?.actor?.name
                if (!actorName.isNullOrBlank()) {
                    com.lagradost.cloudstream3.ui.quicksearch.QuickSearchFragment.pushSearch(activity, actorName)
                }
            }"""
        
        # İçeriği birleştir
        new_content = content[:old_block_start] + new_code + content[old_block_end:]
        
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)
        print("YAMA: Actor arama özelliği BAŞARIYLA eklendi.")
    else:
        print("HATA: Kod bloğu bulunamadı! Lütfen ResultFragmentTv.kt dosyasındaki orijinal satırı kontrol edin.")

if __name__ == "__main__":
    apply_patch()
