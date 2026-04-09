import os
import re
from datetime import datetime

def apply_patch():
    # Z Raporu ve Ayrıntılı Günlük için hazırlık
    report = []
    log_details = []
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    report.append(f"--- Z RAPORU ({now}) ---")
    log_details.append(f"=== MOONCROWN YAMA AYRINTILARI ({now}) ===\n")

    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer (Kumanda Tuşları) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            full_content = f.read()
        
        if "MOONCROWN YAMASI" in full_content:
            report.append("[!] FullScreenPlayer: Yama zaten mevcut.")
        else:
            search_pattern = "open class FullScreenPlayer : SubtitleDownloadActivity() {"
            patch = search_pattern + """
    // --- MOONCROWN YAMASI BASLADI ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isShowingEpisodeOverlay) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    player.handleEvent(CSPlayerEvent.NextEpisode, PlayerEventSource.UI)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    player.handleEvent(CSPlayerEvent.PrevEpisode, PlayerEventSource.UI)
                    return true
                }
                KeyEvent.KEYCODE_MENU -> {
                    toggleEpisodesOverlay(true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    // --- MOONCROWN YAMASI BITTI ---
            """
            with open(full_path, "w", encoding="utf-8") as f:
                f.write(full_content.replace(search_pattern, patch))
            report.append("[SUCCESS] FullScreenPlayer: Tus yamasi uygulandi.")
            log_details.append(f"DOSYA: {full_path}\nISLEM: Yeni onKeyDown fonksiyonu eklendi.\nKONUM: Class baslangici.\n")

    # --- 2. GeneratorPlayer (Canlı TV & HashCode) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            gen_content = f.read()

        if "MOONCROWN YAMASI" in gen_content:
            report.append("[!] GeneratorPlayer: Yama zaten mevcut.")
        else:
            # Senin bulamadığın o karmaşık bloğu (loadLink ve çevresini) hedef alıyoruz
            # Regex: loadLink kelimesinden başlar, sameEpisode = false görene kadar her şeyi (satır atlamaları dahil) kapsar.
            target_regex = r"loadLink\s*\(\s*Pair\s*\(\s*it\s*,\s*null\s*\)\s*,\s*sameEpisode\s*=\s*false\s*\)"
            
            replacement = """// --- MOONCROWN YAMASI BASLADI ---
                    AnySampleMetadata(
                        name = result.name,
                        headerName = result.name,
                        tvType = TvType.Live,
                        parentId = 0,
                        episode = null,
                        season = null,
                        id = result.url.hashCode()
                    ).let { newMeta ->
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
                        player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)
                    }
                    // --- MOONCROWN YAMASI BITTI ---"""

            if re.search(target_regex, gen_content, re.DOTALL):
                # Bulduğumuz o karışık bloğun ne olduğunu günlüğe kaydedelim
                found_block = re.search(target_regex, gen_content, re.DOTALL).group(0)
                
                new_gen_content = re.sub(target_regex, replacement, gen_content, flags=re.DOTALL)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_gen_content)
                
                report.append("[SUCCESS] GeneratorPlayer: Canli TV yamasi uygulandi.")
                log_details.append(f"DOSYA: {gen_path}\nSILINEN KISIM:\n{found_block}\n\nEKLENEN KISIM:\nAnySampleMetadata ve Canli TV Link Kontrolu.\n")
            else:
                report.append("[ERROR] GeneratorPlayer: Hedef blok bulunamadı!")

    # Raporları Dosyaya Yaz
    report.append("--- RAPOR SONU ---")
    final_report = "\n".join(report)
    print(final_report)
    
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)
    
    with open("patch_log.txt", "w", encoding="utf-8") as f:
        f.writelines(log_details)

if __name__ == "__main__":
    apply_patch()
