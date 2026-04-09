import os
import re
from datetime import datetime

def apply_patch():
    report = []
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    report.append(f"--- Z RAPORU ({now}) ---")

    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"

    # --- 1. FullScreenPlayer (Tuş Takımı) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            content = f.read()
        if "MOONCROWN YAMASI" not in content:
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
                f.write(content.replace(search_pattern, patch))
            report.append("[SUCCESS] FullScreenPlayer: Tus yaması eklendi.")
        else:
            report.append("[!] FullScreenPlayer: Yama zaten mevcut.")

    # --- 2. GeneratorPlayer (Esnek Arama Modu) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        if "MOONCROWN YAMASI" in content:
            report.append("[!] GeneratorPlayer: Yama zaten mevcut.")
        else:
            # Hem 'it.let' olanı hem de orijinaldeki bölünmüş satırları yakalayan esnek Regex
            # sameEpisode = false içeren bloğu tamamen hedef alır.
            target_regex = r"set\.firstOrNull\(\)\?\.let\s*\{\s*(?:it\.let\s*\{\s*)?loadLink\s*\(\s*Pair\s*\(\s*it\s*,\s*null\s*\)\s*,\s*sameEpisode\s*=\s*false\s*\)\s*\}?\s*\}"
            
            replacement = """set.firstOrNull()?.let {
                // --- MOONCROWN YAMASI BASLADI: CANLI TV VE HASHCODE ---
                // [SILINDI]: Orijinal loadLink ve it.let yapısı
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
                // --- MOONCROWN YAMASI BITTI ---
            }"""

            if re.search(target_regex, content, re.DOTALL):
                new_content = re.sub(target_regex, replacement, content, count=1, flags=re.DOTALL)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                report.append("[SUCCESS] GeneratorPlayer: Canli TV yaması eklendi.")
            else:
                report.append("[ERROR] GeneratorPlayer: Orijinal kod yapısı bulunamadı!")

    final_report = "\n".join(report)
    print(final_report)
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
