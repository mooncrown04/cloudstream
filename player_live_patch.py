import os
import re

def apply_patch():
    gen_path = "app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt"
    
    if os.path.exists(gen_path):
        with open(gen_path, "r", encoding="utf-8") as f:
            content = f.read()

        if "MOONCROWN YAMASI" in content:
            print("[!] Yama zaten uygulanmış.")
            return

        # SENİN KODUNDAKİ 522. SATIRI HEDEF ALIYORUZ
        # 'private fun loadLink' fonksiyonunun hemen girişine yamayı ekliyoruz.
        target = "private fun loadLink(link: Pair<ExtractorLink?, ExtractorUri?>?, sameEpisode: Boolean) {"
        
        patch = target + """
        // --- MOONCROWN YAMASI BASLADI ---
        val result = viewModel.getMeta() 
        if (result is ResultEpisode && result.name.contains("TV", ignoreCase = true)) {
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
                    source = link?.first?.source ?: "",
                    name = result.name,
                    url = link?.first?.url ?: "",
                    referer = link?.first?.referer ?: "", 
                    quality = Qualities.Unknown.value,
                    type = if (link?.first?.url?.contains(".m3u8") == true) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                    headers = link?.first?.headers ?: emptyMap()
                )
                // Orijinal akışın devam etmesi için değişkenleri güncelliyoruz
                // Bu kısım canlı yayınlar için meta veriyi zorlar
            }
        }
        // --- MOONCROWN YAMASI BITTI ---
        """

        if target in content:
            new_content = content.replace(target, patch)
            with open(gen_path, "w", encoding="utf-8") as f:
                f.write(new_content)
            print("[SUCCESS] GeneratorPlayer 522. satıra yama yapıldı.")
        else:
            print("[ERROR] Hedef fonksiyon (loadLink) bulunamadı!")

if __name__ == "__main__":
    apply_patch()
