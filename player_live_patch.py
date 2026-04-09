import os
import re
from datetime import datetime

def apply_patch():
    report = []
    report.append(f"--- Z RAPORU ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')}) ---")
    
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer: Tuş Kontrolü ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            full_content = f.read()
        if "KeyEvent.KEYCODE_DPAD_UP" in full_content:
            report.append("[!] FullScreenPlayer: Yama zaten mevcut.")
        else:
            search_pattern = "open class FullScreenPlayer : SubtitleDownloadActivity() {"
            patch = search_pattern + """
    // --- MOONCROWN YAMASI BASLADI: KUMANDA TUSLARI ---
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
            report.append("[SUCCESS] FullScreenPlayer: Tuş yaması notlarla eklendi.")

    # --- 2. GeneratorPlayer: Canlı TV ve HashCode ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            gen_content = f.read()

        if "val newMeta = AnySampleMetadata" in gen_content:
            report.append("[!] GeneratorPlayer: Yama zaten mevcut.")
        else:
            # Hedef: it.let { loadLink(...) }
            target_pattern = r"it\.let\s*\{\s*loadLink\(Pair\(it, null\), sameEpisode = false\)\s*\}"
            
            # Notlar ve yeni kod
            replacement = """// --- MOONCROWN YAMASI BASLADI: CANLI TV VE HASHCODE ---
                    // SILINDI: it.let { loadLink(Pair(it, null), sameEpisode = false) }
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

            if re.search(target_pattern, gen_content, re.DOTALL):
                new_gen_content = re.sub(target_pattern, replacement, gen_content, flags=re.DOTALL)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_gen_content)
                report.append("[SUCCESS] GeneratorPlayer: Canlı TV mantığı notlarla eklendi.")
            else:
                report.append("[ERROR] GeneratorPlayer: Hedef kod yapısı bulunamadı.")
    
    report.append("--- RAPOR SONU ---")
    final_report = "\n".join(report)
    print(final_report)
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
