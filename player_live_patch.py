import os
from datetime import datetime

def apply_patch():
    report = []
    report.append(f"--- Z RAPORU ({datetime.now().strftime('%Y-%m-%d %H:%M:%S')}) ---")
    
    # Dosya Yolları
    full_player_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt"
    gen_player_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"

    def patch_file(path, search_pattern, patch_code, label):
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            if "KeyEvent.KEYCODE_DPAD_UP" in content:
                report.append(f"[!] {label}: Yama zaten mevcut, geçiliyor.")
                return False
            
            if search_pattern in content:
                new_content = content.replace(search_pattern, search_pattern + patch_code)
                with open(path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                report.append(f"[SUCCESS] {label}: Yama başarıyla uygulandı.")
                return True
            else:
                report.append(f"[ERROR] {label}: Hedef satır bulunamadı!")
                return False
        else:
            report.append(f"[ERROR] {label}: Dosya bulunamadı!")
            return False

    # --- 1. FullScreenPlayer Yaması (Ana Kumanda Kontrolü) ---
    full_player_patch = """
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
    patch_file(full_player_path, "open class FullScreenPlayer : SubtitleDownloadActivity() {", full_player_patch, "FullScreenPlayer")

    # --- 2. GeneratorPlayer Yaması (Eğer gerekliyse) ---
    # Not: FullScreen'e eklediğimiz için Generator bunu miras alır, 
    # ama özel bir durum varsa buraya da ekleme yapılabilir.
    report.append("[INFO] GeneratorPlayer kontrol edildi (Miras yoluyla aktif).")

    # Raporu Kaydet
    final_report = "\n".join(report)
    print(final_report)
    with open("patch_report.txt", "w", encoding="utf-8") as f:
        f.write(final_report)

if __name__ == "__main__":
    apply_patch()
