import os

def apply_patch():
    file_path = "app/src/main/java/com/lagradost/cloudstream3/ui/result/ResultFragmentTv.kt"
    
    if not os.path.exists(file_path):
        print("HATA: Dosya yolu bulunamadı.")
        return

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # ÇIKMAZA GİRMEYİ ENGELLEYEN KONTROL:
    # Eğer "ActorData" kelimesi dosyada zaten varsa, yama yapılmış demektir.
    if "com.lagradost.cloudstream3.ActorData" in content:
        print("BİLGİ: Yama zaten uygulanmış, tekrar işlem yapılmadı.")
        return

    # Aranan orijinal satır
    search_text = "resultCastItems.adapter = ActorAdaptor(aboveCast?.id) {"
    
    if search_text in content:
        # Eski bloğun başlangıcını bul
        start_idx = content.find(search_text)
        # Orijinal bloğun bittiği yerdeki ilk '}' işaretini bul
        end_idx = content.find("}", start_idx) + 1
        
        # Yeni kod bloğu (Senin istediğin özellik)
        new_code = """resultCastItems.adapter = ActorAdaptor(aboveCast?.id) { view ->
                toggleEpisodes(false)
                val actorName = (view?.tag as? com.lagradost.cloudstream3.ActorData)?.actor?.name
                if (!actorName.isNullOrBlank()) {
                    com.lagradost.cloudstream3.ui.quicksearch.QuickSearchFragment.pushSearch(activity, actorName)
                }
            }"""
        
        # Dosyayı yeniden birleştir
        updated_content = content[:start_idx] + new_code + content[end_idx:]
        
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(updated_content)
        print("BAŞARI: Dosya güvenli bir şekilde güncellendi.")
    else:
        print("HATA: Aranan kod bloğu bulunamadı. Orijinal dosya yapısı değişmiş olabilir.")

if __name__ == "__main__":
    apply_patch()
