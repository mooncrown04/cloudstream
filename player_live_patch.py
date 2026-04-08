import os
import re

def apply_player_patch():
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer.kt (D-Pad Akıllı Kısayol Sistemi) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
        
        new_lines = []
        found_keydown = False
        
        for line in lines:
            new_lines.append(line)
            # Tuş yakalama fonksiyonunun başlangıcını buluyoruz
            if "override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {" in line and not found_keydown:
                # KRİTİK KONTROL: Eğer menü/overlay görünmüyorsa tuşlar doğrudan kanal/bölüm değiştirir
                new_lines.append("        if (!isShowingEpisodeOverlay) {\n")
                new_lines.append("            when (keyCode) {\n")
                new_lines.append("                KeyEvent.KEYCODE_DPAD_UP -> {\n")
                # PlayerEventSource.UI parametresi, kanal isminin altta görünmesini sağlar
                new_lines.append("                    player.handleEvent(CSPlayerEvent.NextEpisode, PlayerEventSource.UI)\n")
                new_lines.append("                    return true\n")
                new_lines.append("                }\n")
                new_lines.append("                KeyEvent.KEYCODE_DPAD_DOWN -> {\n")
                new_lines.append("                    player.handleEvent(CSPlayerEvent.PrevEpisode, PlayerEventSource.UI)\n")
                new_lines.append("                    return true\n")
                new_lines.append("                }\n")
                new_lines.append("            }\n")
                new_lines.append("        }\n")
                found_keydown = True

        with open(full_path, "w", encoding="utf-8") as f:
            f.writelines(new_lines)
        print("FullScreenPlayer: Akıllı D-Pad geçiş sistemi uygulandı.")

    # --- 2. GeneratorPlayer.kt (Canlı Yayın & Önerilenler Uyumu) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Senin sağladığın canlı yayın ve hashCode yapısı
        # Bu yapı sayesinde 'NextEpisode' komutu geldiğinde sistem bir sonraki 'Önerilen' kanala geçer.
        target_code = """                    val newMeta = AnySampleMetadata(
                        name = result.name,
                        headerName = result.name,
                        tvType = TvType.Live,
                        parentId = 0,
                        episode = null,
                        season = null,
                        id = result.url.hashCode()
                    )                    
                    currentMeta = newMeta
       
                    val linkToLoad = ExtractorLink(
                        source = apiSource,
                        name = result.name,
                        url = result.url,
                        referer = "", 
                        quality = Qualities.Unknown.value,
                        type = if (result.url.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                        headers = emptyMap()
                    )

                    loadLink(Pair(linkToLoad, null), sameEpisode = false)
                    player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)"""

        # Orijinal dosyada senin dokunduğun yeri bulup güncelliyoruz
        pattern = r'val newMeta = AnySampleMetadata\(.*?\).*?loadLink\(.*?\)'
        if "loadLink" in content:
            updated_content = re.sub(pattern, target_code, content, flags=re.DOTALL)
            with open(gen_path, "w", encoding="utf-8") as f:
                f.write(updated_content)
            print("GeneratorPlayer: Canlı yayın ve Önerilenler (Recommendations) desteği güncellendi.")

if __name__ == "__main__":
    apply_player_patch()
