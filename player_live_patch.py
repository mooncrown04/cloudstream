import os
import re
from datetime import datetime

def apply_patch():
    report = []
    report.append(f"--- Z RAPORU ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')}) ---")
    
    # Dosya yolları (Depondaki orijinal isimler)
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer Yaması (Kumanda Tuşları) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            full_content = f.read()
        
        if "MOONCROWN YAMASI" in full_content:
            report.append("[!] FullScreenPlayer: Yama zaten mevcut, geçiliyor.")
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
            report.append("[SUCCESS] FullScreenPlayer: Tuş yaması eklendi.")
    else:
        report.append("[ERROR] FullScreenPlayer.kt bulunamadı!")

    # --- 2. GeneratorPlayer Yaması (Esnek Eşleştirme Sistemi) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            gen_content = f.read()

        if "MOONCROWN YAMASI" in gen_content:
            report.append("[!] GeneratorPlayer: Yama zaten mevcut, geçiliyor.")
        else:
            # HEDEF: 'it.let' ile başlayıp 'loadLink' ve 'sameEpisode = false' ile biten tüm bloğu yakalar
            # Bu regex, aradaki tüm boşlukları veya farklı karakterleri görmezden gelir.
            target_regex = r"it\.let\s*\{\s*loadLink\(Pair\(it,\s*null\),\s*sameEpisode\s*=\s*false\)\s*\}"
            
            replacement = """// --- MOONCROWN YAMASI BASLADI: CANLI TV VE HASHCODE ---
                    // [SİLİNDİ]: it.let { loadLink(Pair(it, null), sameEpisode = false) }
                    // [EKLENDİ]: Canlı TV mantığı ve HashCode ID sistemi aşağıya eklendi
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
                new_gen_content = re.sub(target_regex, replacement, gen_content, flags=re.DOTALL)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_gen_content)
                report.append("[SUCCESS] GeneratorPlayer: Canlı TV yaması notlarla eklendi.")
            else:
                report.append("[ERROR] GeneratorPlayer: Hedef kod yapısı bulunamadı (Regex uyuşmazlığı).")
    else:
        report.append("[ERROR] GeneratorPlayer.kt bulunamadı!")

    report.append("--- RAPOR SONU ---")
    
    final_report = "\n".join(report)
    print(final_report)
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
