import os
import re
from datetime import datetime

def apply_patch():
    report = []
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    report.append(f"--- Z RAPORU ({now}) ---")

    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"

    # --- 1. FullScreenPlayer (Kumanda Yaması) ---
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
            report.append("[SUCCESS] FullScreenPlayer yamalandı.")

    # --- 2. GeneratorPlayer (Esnek "Mıknatıs" Arama) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        if "MOONCROWN YAMASI" in content:
            report.append("[!] GeneratorPlayer: Yama zaten mevcut.")
        else:
            # DİKKAT: Artık 'false' kelimesini aramıyoruz!
            # Sadece linklerin ilk defa yüklendiği 'loadLink(Pair(it...' bloğunu hedef alıyoruz.
            target_regex = r"loadLink\s*\(\s*Pair\s*\(\s*it\s*,\s*null\s*\).*?\)"
            
            replacement = """// --- MOONCROWN YAMASI BASLADI ---
                // [SILINDI]: Orijinal loadLink çağrısı
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

            if re.search(target_regex, content, re.DOTALL):
                # Sadece ilk karşılaştığı yeri (ana oynatma başlatıcıyı) değiştirir.
                new_content = re.sub(target_regex, replacement, content, count=1, flags=re.DOTALL)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                report.append("[SUCCESS] GeneratorPlayer yamalandı.")
            else:
                report.append("[ERROR] GeneratorPlayer: 'loadLink(Pair(it, null))' bulunamadı.")

    print("\n".join(report))

if __name__ == "__main__":
    apply_patch()
