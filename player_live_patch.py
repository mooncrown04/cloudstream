import os
import re
from datetime import datetime

def apply_patch():
    report = []
    report.append(f"--- Z RAPORU ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')}) ---")
    
    # Orijinal dosya yolları
    full_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    # --- 1. FullScreenPlayer Yaması (Zaten Başarılı) ---
    if os.path.exists(full_path):
        with open(full_path, "r", encoding="utf-8") as f:
            full_content = f.read()
        
        if "MOONCROWN YAMASI" in full_content:
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
            report.append("[SUCCESS] FullScreenPlayer: Tuş yaması eklendi.")

    # --- 2. GeneratorPlayer Yaması (Satır Bazlı Garantili Yöntem) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        if any("MOONCROWN YAMASI" in line for line in lines):
            report.append("[!] GeneratorPlayer: Yama zaten mevcut.")
        else:
            new_lines = []
            patched = False
            i = 0
            while i < len(lines):
                # Hedef satırı bul: loadLink ve sameEpisode = false içeren satır
                if "loadLink" in lines[i] and "sameEpisode = false" in lines[i] and not patched:
                    # Bu satırı ve onu sarmalayan 'it.let {' bloğunu değiştireceğiz.
                    # Genelde bir önceki satır 'it.let {' olur.
                    if "it.let {" in lines[i-1]:
                        new_lines.pop() # 'it.let {' satırını sil/çıkar
                        
                    new_lines.append("                    // --- MOONCROWN YAMASI BASLADI: CANLI TV VE HASHCODE ---\n")
                    new_lines.append("                    // [SILINDI]: it.let { loadLink(Pair(it, null), sameEpisode = false) }\n")
                    new_lines.append("                    // [EKLENDI]: Canlı TV mantığı ve HashCode ID sistemi eklendi\n")
                    new_lines.append("""                    AnySampleMetadata(
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
                    }\n""")
                    new_lines.append("                    // --- MOONCROWN YAMASI BITTI ---\n")
                    
                    # Eğer it.let bloğu tek satırda değilse ve sonunda '}' varsa onu atla
                    if "}" in lines[i]: 
                        pass # Satırın kendisini zaten değiştirdik
                    elif i+1 < len(lines) and "}" in lines[i+1]:
                        i += 1 # Bir sonraki kapatma parantezini atla
                        
                    patched = True
                else:
                    new_lines.append(lines[i])
                i += 1

            if patched:
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.writelines(new_lines)
                report.append("[SUCCESS] GeneratorPlayer: Canlı TV yaması notlarla eklendi.")
            else:
                report.append("[ERROR] GeneratorPlayer: Hedef satır (loadLink) bulunamadı!")

    report.append("--- RAPOR SONU ---")
    final_report = "\n".join(report)
    print(final_report)
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
