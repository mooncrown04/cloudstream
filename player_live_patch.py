import os

def apply_patch():
    # Dosya adını senin paylaştığın "GeneratorPlayer (1).kt" olarak veya 
    # projedeki orijinal adı olan "GeneratorPlayer.kt" olarak düzeltebilirsin.
    path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    
    if not os.path.exists(path):
        path = "GeneratorPlayer (1).kt" # Test için senin yüklediğin isim

    if not os.path.exists(path):
        print("[HATA] Dosya bulunamadı!")
        return

    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    # Orijinal dosyadaki 454. satır ve çevresi tam olarak budur:
    old_code = """        observe(viewModel.currentLinks) { set ->
            set.firstOrNull()?.let {
                loadLink(Pair(it, null))
            }
        }"""

    # Canlı TV desteği eklenmiş yeni hali:
    new_code = """        observe(viewModel.currentLinks) { set ->
            set.firstOrNull()?.let {
                // --- MOONCROWN CANLI TV YAMASI ---
                val result = viewModel.getMeta()
                if (result is ResultEpisode) {
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
                            source = it.source,
                            name = result.name,
                            url = it.url,
                            referer = it.referer,
                            quality = Qualities.Unknown.value,
                            type = if (it.url.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                            headers = it.headers
                        )
                        loadLink(Pair(linkToLoad, null), sameEpisode = false)
                        player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)
                    }
                } else {
                    loadLink(Pair(it, null))
                }
                // --- MOONCROWN CANLI TV YAMASI BITTI ---
            }
        }"""

    if old_code in content:
        updated_content = content.replace(old_code, new_code)
        with open(path, "w", encoding="utf-8") as f:
            f.write(updated_content)
        print("[SUCCESS] GeneratorPlayer başarıyla yamalandı!")
    else:
        print("[ERROR] Orijinal kod yapısı bulunamadı. Lütfen dosyanın değiştirilmediğinden emin ol.")

if __name__ == "__main__":
    apply_patch()
