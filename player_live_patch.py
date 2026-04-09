import os
import re
from datetime import datetime

def apply_patch():
    report = []
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    report.append(f"--- Z RAPORU ({now}) ---")

    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        if "MOONCROWN YAMASI" in content:
            report.append("[!] GeneratorPlayer: Yama zaten uygulanmış.")
        else:
            # DİKKAT: Senin dosyandaki 'loadLink(Pair(it, null))' yapısını yakalar.
            # İçinde 'false' olsa da olmasa da bu regex orayı bulur.
            target_regex = r"set\.firstOrNull\(\)\?\.let\s*\{\s*loadLink\s*\(\s*Pair\s*\(\s*it\s*,\s*null\s*\)\s*\)\s*\}"
            
            replacement = """set.firstOrNull()?.let {
                // --- MOONCROWN YAMASI BASLADI ---
                // [SILINDI]: loadLink(Pair(it, null))
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
                report.append("[ERROR] GeneratorPlayer: Orijinal kod (loadLink) bulunamadı!")

    print("\n".join(report))

if __name__ == "__main__":
    apply_patch()
