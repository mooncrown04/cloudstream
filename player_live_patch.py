import os

def apply_cloudstream_player_patch():
    # Düzenlenecek dosya yolları
    generator_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    player_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"

    # --- 1. GeneratorPlayer.kt Yaması (Live TV & HashID Desteği) ---
    if os.path.exists(generator_path):
        with open(generator_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
        
        new_lines = []
        found = False
        i = 0
        while i < len(lines):
            line = lines[i]
            # Orijinal meta yükleme kısmını bul
            if "val newMeta = Any" in line or "currentMeta = newMeta" in line:
                # Senin yazdığın Live TV ve hashID mantığını enjekte et
                new_lines.append("                    val newMeta = AnySampleMetadata(\n")
                new_lines.append("                        name = result.name,\n")
                new_lines.append("                        headerName = result.name,\n")
                new_lines.append("                        tvType = TvType.Live,\n")
                new_lines.append("                        parentId = 0,\n")
                new_lines.append("                        episode = null,\n")
                new_lines.append("                        season = null,\n")
                new_lines.append("                        id = result.url.hashCode()\n")
                new_lines.append("                    )\n")
                new_lines.append("                    currentMeta = newMeta\n")
                
                # M3U8 kontrolü yapan extractor kısmını ekle
                new_lines.append("                    val linkToLoad = ExtractorLink(\n")
                new_lines.append("                        source = apiSource,\n")
                new_lines.append("                        name = result.name,\n")
                new_lines.append("                        url = result.url,\n")
                new_lines.append("                        referer = \"\",\n")
                new_lines.append("                        quality = Qualities.Unknown.value,\n")
                new_lines.append("                        type = if (result.url.contains(\".m3u8\")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,\n")
                new_lines.append("                        headers = emptyMap()\n")
                new_lines.append("                    )\n")
                new_lines.append("                    loadLink(Pair(linkToLoad, null), sameEpisode = false)\n")
                new_lines.append("                    player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)\n")
                
                # Orijinal bloğu atla (eski loadLink veya meta tanımlarını geç)
                while i < len(lines) and "loadLink" not in lines[i]:
                    i += 1
                found = True
            else:
                new_lines.append(line)
            i += 1
        
        if found:
            with open(generator_path, "w", encoding="utf-8") as f:
                f.writelines(new_lines)
            print("YAMA: GeneratorPlayer.kt (Live TV) başarıyla güncellendi.")

    # --- 2. FullScreenPlayer.kt Yaması (D-Pad Kanal Değiştirme) ---
    if os.path.exists(player_path):
        with open(player_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        new_lines = []
        found_dpad = False
        i = 0
        while i < len(lines):
            line = lines[i]
            # KeyDown olayını bul ve D-Pad tuşlarını ekle
            if "when (keyCode) {" in line:
                new_lines.append(line)
                new_lines.append("                KeyEvent.KEYCODE_DPAD_UP -> {\n")
                new_lines.append("                    toggleEpisodesOverlay(true)\n")
                new_lines.append("                    true\n")
                new_lines.append("                }\n")
                new_lines.append("                KeyEvent.KEYCODE_DPAD_DOWN -> {\n")
                new_lines.append("                    toggleEpisodesOverlay(true)\n")
                new_lines.append("                    true\n")
                new_lines.append("                }\n")
                found_dpad = True
            else:
                new_lines.append(line)
            i += 1

        if found_dpad:
            with open(player_path, "w", encoding="utf-8") as f:
                f.writelines(new_lines)
            print("YAMA: FullScreenPlayer.kt (D-Pad Navigasyonu) başarıyla güncellendi.")

if __name__ == "__main__":
    apply_cloudstream_player_patch()