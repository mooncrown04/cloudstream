import os
from datetime import datetime

def apply_patch():
    report = []
    report.append(f"--- Z RAPORU ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')}) ---")
    
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    
    if os.path.exists(gen_path):
        report.append(f"[OK] Dosya bulundu: {gen_path}")
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        if "KeyEvent.KEYCODE_DPAD_UP" in content:
            report.append("[!] UYARI: D-Pad yaması zaten mevcut, tekrar uygulanmadı.")
        else:
            target = "class GeneratorPlayer : FullScreenPlayer() {"
            patch = target + """
    // --- KUMANDA TUSLARI VE CANLI TV YAMASI ---
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
            if target in content:
                new_content = content.replace(target, patch)
                with open(gen_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                report.append("[SUCCESS] D-Pad ve Menü tuşları başarıyla eklendi.")
            else:
                report.append("[ERROR] Hedef satır bulunamadı! Dosya yapısı değişmiş olabilir.")
    else:
        report.append(f"[ERROR] Dosya bulunamadı: {gen_path}")

    report.append("--- RAPOR SONU ---")
    
    # Raporu hem ekrana yazdır hem de dosyaya kaydet
    final_report = "\n".join(report)
    print(final_report)
    
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
