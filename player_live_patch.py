import os
import re
from datetime import datetime

def apply_patch():
    report = []
    report.append(f"--- Z RAPORU ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')}) ---")
    
    # Dosya Yolları
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer Yaması (D-Pad Tuşları) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        if "KeyEvent.KEYCODE_DPAD_UP" in content:
            report.append("[!] FullScreenPlayer: Tuş yaması zaten mevcut.")
        else:
            search_pattern = "open class FullScreenPlayer : SubtitleDownloadActivity() {"
            patch = search_pattern + """
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
            """
            if search_pattern in content:
                with open(full_path, "w", encoding="utf-8") as f:
                    f.write(content.replace(search_pattern, patch))
                report.append("[SUCCESS] FullScreenPlayer: Tuş yaması uygulandı.")
            else:
                report.append("[ERROR] FullScreenPlayer: Hedef satır bulunamadı!")
    else:
        report.append("[ERROR] FullScreenPlayer.kt bulunamadı!")

    # --- 2. GeneratorPlayer Yaması (Canlı TV & HashCode - ÖZEL ENJEKSİYON) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        if "val newMeta = AnySampleMetadata" in content:
            report.append("[!] GeneratorPlayer: Canlı TV yaması zaten mevcut.")
        else:
            # Senin paylaştığın dosyadaki spesifik bloğu hedef alıyoruz
            target_regex = r"it\.toMetadata\(\)\.let\s*\{\s*meta\s*->.*?loadLink\(Pair\(it, null\), sameEpisode = false\)\s*\}"
            
            replacement = """AnySampleMetadata(
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
                    }"""

            if "it.toMetadata()" in content:
                # Regex ile bloğu bulup değiştiriyoruz
                new_content = re.sub(target_regex, replacement, content, flags=re.DOTALL)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                report.append("[SUCCESS] GeneratorPlayer: Canlı TV ve HashCode mantığı eklendi.")
            else:
                report.append("[ERROR] GeneratorPlayer: 'it.toMetadata()' bloğu bulunamadı!")
    else:
        report.append("[ERROR] GeneratorPlayer.kt bulunamadı!")

    report.append("--- RAPOR SONU ---")
    
    # Raporu yazdır ve dosyaya kaydet
    final_report = "\n".join(report)
    print(final_report)
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
