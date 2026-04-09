import os
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
            report.append("[SUCCESS] FullScreenPlayer yamalandı.")

    # --- 2. GeneratorPlayer (Mevcut Yamayı Düzeltme) ---
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Eğer zaten bir yama denemesi varsa onu temizleyip en güncel halini koyalım
        if "AnySampleMetadata" in content:
            report.append("[!] GeneratorPlayer: Eski yama bulundu, güncelleniyor...")
            # Bu kısımda eskiyi silmek yerine, eksik olan 'MOONCROWN' etiketlerini ekleyelim
            if "MOONCROWN YAMASI" not in content:
                content = content.replace("AnySampleMetadata(", "// --- MOONCROWN YAMASI BASLADI ---\n                    AnySampleMetadata(")
                content = content.replace("player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)", "player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)\n                    // --- MOONCROWN YAMASI BITTI ---")
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(content)
                report.append("[SUCCESS] GeneratorPlayer notlar eklendi.")
            else:
                report.append("[!] GeneratorPlayer: Yama zaten tam görünüyor.")
        else:
            report.append("[ERROR] GeneratorPlayer: Beklenen kod yapısı bulunamadı.")

    print("\n".join(report))

if __name__ == "__main__":
    apply_patch()
