package com.lagradost.cloudstream3.ui.settings

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.text.style.TypefaceSpan
import android.view.View
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchQuality
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.BasePreferenceFragmentCompat
import com.lagradost.cloudstream3.ui.clear
import com.lagradost.cloudstream3.ui.home.HomeChildItemAdapter
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.search.SearchAdapter
import com.lagradost.cloudstream3.ui.search.SearchResultBuilder
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.updateTv
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.getPref
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.hideOn
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setPaddingBottom
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setToolBarScrollFlags
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setUpToolbar
importHata, Android / Jetpack Preferences (veya custom preference) yapısında `entries` ve `entryValues` parametrelerinin veri tipleri arasındaki uyumsuzluktan kaynaklanıyor. 

`CharSequence` tipindeki bir listeyi (örneğin `entries.map { ... }` veya `resources.getStringArray(...)` sonucu dönen yapıyı), `List<String>` bekleyen bir fonksiyona veya parametreye paslamışsınız.

Sorunu çözmek için ilgili kısımdaki listeyi explicit olarak `map { it.toString() }` veya `.toTypedArray()` ile `String` tipine dönüştürmeniz gerekir.

### Düzeltilmiş Kod (`SettingsUI.kt` - 304. Satır Çevresi)

Hatanın oluştuğu `ListPreference` / ayar bölümünü şu şekilde güncelleyin:

```kotlin
// EĞER List<CharSequence> tipindeki diziyi String listesine çeviriyorsanız:
val stringList: List<String> = charSequenceList.map { it.toString() }

// EĞER ListPreference için entries ve entryValues ataması yapıyorsanız:
// (Örnek: Font veya Tema seçici yapısı)
val fontNames: List<CharSequence> = arrayOf("Default", "Custom Font 1", "Custom Font 2")
val fontValues: List<String> = arrayOf("default", "font_1", "font_2").toList()

// Hata veren fonksiyon çağrısı veya preference tanımı:
setupCustomPreference(
    entries = fontNames.map { it.toString() }, // CharSequence -> String dönüşümü
    entryValues = fontValues
)
