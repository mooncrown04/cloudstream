import os
import re

def apply_player_patch():
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer.kt (D-Pad + Opsiyon Tuşu) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
        
        new_lines = []
        keydown_patched = False
        
        for line in lines:
            new_lines.append(line)
            
            # onKeyDown içine senin çalışan mantığını ve Opsiyon tuşunu ekliyoruz
            if "override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {" in line and not keydown_patched:
                new_lines.append("        if (!isShowingEpisodeOverlay) {\n")
                new_lines.append("            when (keyCode) {\n")
                # Üst Tuş: Sonraki Kanal
                new_lines.append("                KeyEvent.KEYCODE_DPAD_UP -> {\n")
                new_lines.append("                    player.handleEvent(CSPlayerEvent.NextEpisode, PlayerEventSource.UI)\n")
                new_lines.append("                    return true\n")
                new_lines.append("                }\n")
                # Alt Tuş: Önceki Kanal
                new_lines.append("                KeyEvent.KEYCODE_DPAD_DOWN -> {\n")
                new_lines.append("                    player.handleEvent(CSPlayerEvent.PrevEpisode, PlayerEventSource.UI)\n")
                new_lines.append("                    return true\n")
                new_lines.append("                }\n")
                # 3 Çizgili Opsiyon Tuşu: Sekmeleri/Menüyü Aç
                new_lines.append("                KeyEvent.KEYCODE_MENU -> {\n")
                new_lines.append("                    toggleEpisodesOverlay(true)\n")
                new_lines.append("                    return true\n")
                new_lines.append("                }\n")
                new_lines.append("            }\n")
                new_lines.append("        }\n")
                keydown_patched = True

        with open(full_path, "w", encoding="utf-8") as f:
            f.writelines(new_lines)
        print("FullScreenPlayer: D-Pad ve Opsiyon (Menu) tuşu sisteme eklendi.")

    # --- 2. GeneratorPlayer.kt (Senin Çalışan Canlı TV Mantığın) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Senin çalışan hashCode ve Live TV blok yapın
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

        # Orijinal dosyada değiştirilecek alanı senin kodunla günceller
        pattern = r'val newMeta = AnySampleMetadata\(.*?\)\s+currentMeta = newMeta.*?loadLink\(.*?\)'
        if "loadLink" in content:
            updated_content = re.sub(pattern, target_code, content, flags=re.DOTALL)
            with open(gen_path, "w", encoding="utf-8") as f:
                f.write(updated_content)
            print("GeneratorPlayer: Senin çalışan canlı TV yapın uygulandı.")

if __name__ == "__main__":
    apply_player_patch()
