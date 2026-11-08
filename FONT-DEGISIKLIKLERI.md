# CloudStream — Font Düzeltmesi: Değişiklik Notları

**Repo:** `mooncrown04/cloudstream` · **Branch:** `master`
**Başlangıç commit'i:** `32d1e2c` (Update strings.xml)
**Son commit:** `83f5848`
**Toplam:** 7 commit, 12 dosya, +348 / −137 satır

---

## Sorun neydi?

Ayarlar > Arayüz > **App Font** seçimi kaydediliyordu ama **hiçbir yerde uygulanmıyordu**:

| Belirti | Gerçek sebep |
|---|---|
| Ana sayfa "seçtiğim font" gibi görünüyordu | Aslında `AppTextViewStyle`'daki sabit Google Sans (Product Sans) |
| Ayarlar menüsündeki yazılar değişmiyordu | Compose tarafında `CloudStreamTheme(fontFamily = …)` hiç verilmiyordu |
| Diyaloglar değişmiyordu | Diyalog ayrı pencere açar, üstteki `MaterialTheme`'i miras almaz |
| Bazı fontlar hiç çalışmıyordu | `perfToFont()` yalnızca 5 fontu tanıyordu |
| `getAppFontFamily()` | Repoda **tek çağrısı yoktu** — ölü kod |

---

## Commit listesi (kronolojik)

### 1. `c531898` — fix(font): App Font ayarını tüm Compose ekranlarına ve diyaloglara uygula
İlk düzeltme. Font seçimi artık gerçekten okunuyor.

- `shared/theme/Theme.kt` — `LocalAppFontFamily` composition local eklendi; `CloudStreamTheme` bunu sağlıyor
- `shared/theme/AppFont.kt` (yeni) — `rememberAppFontFamily()`, `CloudStreamFontTheme()`, `expect fun resolveAppFontFamily()`
- `shared/theme/AppFont.android.kt` / `AppFont.jvm.kt` (yeni) — actual tanımlar
- `shared/compose/ComposeFragment.kt` — kök ComposeView'a font bağlandı
- `shared/compose/Dialog.kt` — `ActionDialog`, `SingleSelectDialog`, `MultiSelectDialog` font sarmalayıcısına alındı
- `shared/theme/Font.kt` — `null` iken varsayılan `Typography` korunuyor
- `shared/theme/ColorPreference.kt` — `perfToFont` 10 fontu da tanıyor
- `app/utils/FontUtils.kt` — reaktif yardımcılar
- `app/ui/settings/SettingsUIScreen.kt` — yinelenen font haritası kaldırıldı
- `app/res/values/styles.xml` — overlay stilleri genişletildi

### 2. `b1f5036` — fix(font): ayarlar ekranındaki yazıların fontu değişmiyordu
Asıl suçlu bulundu: `style.copy(fontSize = …)` çağrısı.

- `BasePreferenceWidget.kt` — `MaterialTheme.typography.titleLarge.copy(fontSize = …)` yerine `style` + ayrı `fontSize` parametresi
- `CloudStreamTheme` — `fontFamily` varsayılanı `rememberAppFontFamily()` oldu (tüm ağaç kapsanıyor)
- `Typography.withFontFamily()` — 15 stilin tamamına fontu açıkça uygular
- `resolveAppFontFamily` — hatalar artık logcat'e yazılıyor (`AppFont` etiketi)

### 3. `b672c3c` — fix(build): styles.xml'de var olmayan item isimleri build'i kırıyordu
> ⚠️ Bu commit ile birlikte derleme hataları başladı; aşağıdaki iki commit onları giderdi.

- Overlay'lerdeki `android:textAppearanceListItem`, `textViewStyle`, `buttonStyle`, `editTextStyle`, `textAppearanceLarge/Medium/Small`, `materialButtonStyle` item'ları kaldırıldı

### 4. `279e2aa` — fix(build): AAPT2 resource linking failed
- `preferenceStyle`, `switchPreferenceCompatStyle`, `seekBarPreferenceStyle`, `editTextPreferenceStyle` ve `@layout/preference_category_material` referansları kaldırıldı
- `AppTheme`'teki `android:fontFamily` geri alındı
- Overlay'ler **bilinen çalışan** hâline döndü: sadece `android:fontFamily` + `fontFamily` + `preferenceCategoryStyle`

### 5. `e7f62b6` — refactor(font): basitleştir
- `rememberAppFontKey()` kaldırıldı, anahtar okuma `rememberAppFontFamily()` içine alındı
- Kullanılmayan `AppFontTheme` sarmalayıcısı silindi
- `CloudStreamFontTheme` içindeki `ProvideTextStyle` kaldırıldı

### 6. `05e86b9` — fix(build): shared modülü app'in R.font'una erişemiyordu
Hata: `Unresolved reference 'getAppFontFamily'` — `shared` modülü `app`'e bağımlı değil.

- Font dosyaları artık **kaynak adıyla** yükleniyor:
  `context.resources.getIdentifier(fileName, "font", context.packageName)`
- `appFontFileNames` haritası (anahtar → dosya adı) tek kaynak oldu
- `resolveAppFontFamily` `@Composable` yapıldı (`LocalContext` kullanıyor)
- `jvm` actual'ı da `@Composable` işaretlendi

### 7. `83f5848` — fix(build): SettingsUIScreen'de bozulan fonksiyon yapısı
- İç içe girmiş ikinci `getTitleRes()` tanımı kaldırıldı
- `androidx.compose.ui.Modifier` importu geri eklendi
- Kullanılmayan `collectAsState` / `getValue` importları temizlendi

---

## Değişen dosyaların nihai hâli

| Dosya | Ne yapıyor |
|---|---|
| `shared/.../theme/AppFont.kt` **(yeni)** | `rememberAppFontFamily()` · `expect resolveAppFontFamily()` · `CloudStreamFontTheme()` |
| `shared/.../theme/AppFont.android.kt` **(yeni)** | `appFontFileNames` haritası · `getIdentifier` ile font yükleme |
| `shared/.../theme/AppFont.jvm.kt` **(yeni)** | Masaüstünde her zaman `null` |
| `shared/.../theme/Theme.kt` | `LocalAppFontFamily` · `CloudStreamTheme(fontFamily = rememberAppFontFamily())` |
| `shared/.../theme/Font.kt` | `rememberAppTypography()` · `Typography.withFontFamily()` |
| `shared/.../theme/ColorPreference.kt` | `knownAppFontKeys` · `perfToFont` tüm fontları tanıyor |
| `shared/.../compose/ComposeFragment.kt` | Kök ComposeView (font artık tema içinden geliyor) |
| `shared/.../compose/Dialog.kt` | 3 diyalog `CloudStreamFontTheme` ile sarıldı |
| `shared/.../settings/widget/BasePreferenceWidget.kt` | `.copy(fontSize=…)` kaldırıldı — **ayarlar ekranı düzeltmesi** |
| `app/.../utils/FontUtils.kt` | `getAppFontFamily()` — anahtar → `R.font` eşlemesi (XML tarafı için) |
| `app/.../ui/settings/SettingsUIScreen.kt` | Yinelenen font haritası kaldırıldı, `recreate()` gereksizleşti |
| `app/src/main/res/values/styles.xml` | Overlay'ler sadeleştirildi (10 font × 2 stil) |

---

## Yeni font eklemek için

1. `.ttf` dosyasını `app/src/main/res/font/` altına koy
2. `app/src/main/res/values/array.xml` → `app_font_values` + `app_font_names` listelerine ekle
3. `app/.../utils/FontUtils.kt` → `getAppFontFamily()` içine ekle (XML tarafı)
4. `shared/.../theme/AppFont.android.kt` → `appFontFileNames` haritasına ekle (Compose tarafı)
5. `app/src/main/res/values/styles.xml` → `<FontAdı>FontOverlay` + `<FontAdı>PrefCategory` ekle

---

## Derleme durumu

Son doğrulanan GitHub Actions log'u: **`83f5848`** commit'i henüz derlenmedi.
Önceki log'daki hata (`SettingsUIScreen.kt:76 Unresolved reference 'Modifier'`) giderildi.

**Derlemede hata çıkarsa:** `grep -i "e: file" build.log | head -20` çıktısı yeterli —
`BUILD FAILED` satırı tek başına sebebi göstermiyor.

---

## Bilinen açık nokta

`shared` modülü `app`'e bağımlı olmadığı için font eşlemesi **iki yerde** duruyor:

- `app/.../utils/FontUtils.kt` → `getAppFontFamily()` (XML/View ekranları)
- `shared/.../theme/AppFont.android.kt` → `appFontFileNames` (Compose ekranları)

Tek yerde birleştirmek için `appFontFileNames`'i common'a taşıyıp `FontUtils.kt`'yi ona
bağlamak yeterli — ileride yapılabilir.
