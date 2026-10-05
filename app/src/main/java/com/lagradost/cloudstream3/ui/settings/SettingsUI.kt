package com.lagradost.cloudstream3.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.edit
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
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showBottomDialog
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showDialog
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showMultiDialog
import com.lagradost.cloudstream3.utils.UIHelper.hideKeyboard
import com.lagradost.cloudstream3.utils.UIHelper.toPx

class SettingsUI : BasePreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbar(R.string.category_ui)
        setPaddingBottom()
        setToolBarScrollFlags()
        
        //yeni   Font'u Preference öğelerine uygula
        view.post {
            applyFontToAllPreferences()
        }
        //yeni   
        
    }

//yeni
private fun applyFontToAllPreferences() {
    val settingsManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
    val fontKey = settingsManager.getString(
        getString(R.string.app_font_key),
        "Default"
    ) ?: "Default"
    
    val typeface = getTypefaceFromKey(fontKey)
    
    // PreferenceFragmentCompat içindeki RecyclerView (listView) üzerinden font uygulanır
    listView?.let { recyclerView ->
        applyFontToViewHierarchy(recyclerView, typeface)
    }
}

private fun applyFontToViewHierarchy(view: View, typeface: android.graphics.Typeface?) {
    if (typeface == null) return
    
    when (view) {
        is TextView -> view.typeface = typeface
        is android.view.ViewGroup -> {
            for (i in 0 until view.childCount) {
                applyFontToViewHierarchy(view.getChildAt(i), typeface)
            }
        }
    }
}

private fun getTypefaceFromKey(fontKey: String): android.graphics.Typeface? {
    return when (fontKey) {
        "TimesNewRoman" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.times_new_roman)
        "StixGeneral" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.stix_general)
        "ComicSans" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.comic_sans)
        "Maybach" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.maybach)
        "Perfume" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.perfume)
        "Naxmos" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.naxmos)
        "Consola" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.consola)
        "Futura" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.futura)
        "GoogleSans" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.google_sans)
        "Gotham" -> androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.gotham)
        else -> null
    }
}
//yeni

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        hideKeyboard()
        setPreferencesFromResource(R.xml.settings_ui, rootKey)
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        (getPref(R.string.overscan_key)?.hideOn(PHONE or EMULATOR) as? SeekBarPreference)?.setOnPreferenceChangeListener { pref, newValue ->
            val padding = (newValue as? Int)?.toPx ?: return@setOnPreferenceChangeListener true
            (pref.context.getActivity() as? MainActivity)?.binding?.homeRoot?.setPadding(padding, padding, padding, padding)
            return@setOnPreferenceChangeListener true
        }

        getPref(R.string.bottom_title_key)?.setOnPreferenceChangeListener { _, _ ->
            HomeChildItemAdapter.sharedPool.clear()
            ParentItemAdapter.sharedPool.clear()
            SearchAdapter.sharedPool.clear()
            true
        }
  //yeni
        getPref(R.string.wide_poster_key)?.setOnPreferenceChangeListener { _, _ ->
            HomeChildItemAdapter.sharedPool.clear()
            ParentItemAdapter.sharedPool.clear()
            SearchAdapter.sharedPool.clear()
            activity?.recreate()
            true
        }
  //yeni

        getPref(R.string.poster_size_key)?.setOnPreferenceChangeListener { _, newValue ->
            HomeChildItemAdapter.sharedPool.clear()
            ParentItemAdapter.sharedPool.clear()
            SearchAdapter.sharedPool.clear()
            context?.let { HomeChildItemAdapter.updatePosterSize(it, newValue as? Int) }
            true
        }

        getPref(R.string.poster_ui_key)?.setOnPreferenceClickListener {
            val prefNames = resources.getStringArray(R.array.poster_ui_options)
            val keys = resources.getStringArray(R.array.poster_ui_options_values)
            val prefValues = keys.map {
                settingsManager.getBoolean(it, true)
            }.mapIndexedNotNull { index, b ->
                if (b) {
                    index
                } else null
            }

            activity?.showMultiDialog(
                prefNames.toList(),
                prefValues,
                getString(R.string.poster_ui_settings),
                {}
            ) { list ->
                settingsManager.edit {
                    for ((i, key) in keys.withIndex()) {
                        putBoolean(key, list.contains(i))
                    }
                }
                SearchResultBuilder.updateCache(it.context)
            }

            return@setOnPreferenceClickListener true
        }

        getPref(R.string.app_layout_key)?.setOnPreferenceClickListener {
            val prefNames = resources.getStringArray(R.array.app_layout)
            val prefValues = resources.getIntArray(R.array.app_layout_values)

            val currentLayout =
                settingsManager.getInt(getString(R.string.app_layout_key), -1)

            activity?.showBottomDialog(
                items = prefNames.toList(),
                selectedIndex = prefValues.indexOf(currentLayout),
                name = getString(R.string.app_layout),
                showApply = true,
                dismissCallback = {},
                callback = {
                    try {
                        settingsManager.edit {
                            putInt(getString(R.string.app_layout_key), prefValues[it])
                        }
                        context?.updateTv()
                        activity?.recreate()
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            )
            return@setOnPreferenceClickListener true
        }

        getPref(R.string.app_theme_key)?.setOnPreferenceClickListener {
            val prefNames = resources.getStringArray(R.array.themes_names).toMutableList()
            val prefValues = resources.getStringArray(R.array.themes_names_values).toMutableList()
            val removeIncompatible = { text: String ->
                val toRemove = prefValues
                    .mapIndexed { idx, s -> if (s.startsWith(text)) idx else null }
                    .filterNotNull()
                var offset = 0
                toRemove.forEach { idx ->
                    prefNames.removeAt(idx - offset)
                    prefValues.removeAt(idx - offset)
                    offset += 1
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { // remove monet on android 11 and less
                removeIncompatible("Monet")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Remove system on android 9 and less
                removeIncompatible("System")
            }

            val currentLayout =
                settingsManager.getString(getString(R.string.app_theme_key), prefValues.first())

            activity?.showBottomDialog(
                prefNames.toList(),
                prefValues.indexOf(currentLayout),
                getString(R.string.app_theme_settings),
                true,
                {}
            ) {
                try {
                    settingsManager.edit {
                        putString(getString(R.string.app_theme_key), prefValues[it])
                    }
                    activity?.recreate()
                } catch (e: Exception) {
                    logError(e)
                }
            }
            return@setOnPreferenceClickListener true
        }
        getPref(R.string.primary_color_key)?.setOnPreferenceClickListener {
            val prefNames = resources.getStringArray(R.array.themes_overlay_names).toMutableList()
            val prefValues =
                resources.getStringArray(R.array.themes_overlay_names_values).toMutableList()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { // remove monet on android 11 and less
                val toRemove = prefValues
                    .mapIndexed { idx, s -> if (s.startsWith("Monet")) idx else null }
                    .filterNotNull()
                var offset = 0
                toRemove.forEach { idx ->
                    prefNames.removeAt(idx - offset)
                    prefValues.removeAt(idx - offset)
                    offset += 1
                }
            }

            val currentLayout =
                settingsManager.getString(getString(R.string.primary_color_key), prefValues.first())

            activity?.showDialog(
                prefNames.toList(),
                prefValues.indexOf(currentLayout),
                getString(R.string.primary_color_settings),
                true,
                {}
            ) {
                try {
                    settingsManager.edit {
                        putString(getString(R.string.primary_color_key), prefValues[it])
                    }
                    activity?.recreate()
                } catch (e: Exception) {
                    logError(e)
                }
            }
            return@setOnPreferenceClickListener true
        }

    // =========================================================================
        // EKLENEN NOT 2: Font seçimi alanında varsayılan özet (summary) güncellemesi
        // ve seçim sonrasında activity recreate çağrısı.
        // =========================================================================
        getPref(R.string.app_font_key)?.setOnPreferenceClickListener {
            val prefNames = resources.getStringArray(R.array.app_font_names).toMutableList()
            val prefValues = resources.getStringArray(R.array.app_font_values).toMutableList()

            val currentFont = settingsManager.getString(
                getString(R.string.app_font_key),
                prefValues.firstOrNull() ?: "Default"
            ) ?: "Default"

            activity?.showBottomDialog(
                prefNames.toList(),
                prefValues.indexOf(currentFont).let { if (it != -1) it else 0 },
                getString(R.string.app_font_settings),
                true,
                {}
            ) { index ->
                try {
                    prefValues.getOrNull(index)?.let { selectedFont ->
                        settingsManager.edit {
                            putString(getString(R.string.app_font_key), selectedFont)
                        }
                        // Activity baştan başlatılır ve yeni font overlay teması yüklenir
                        activity?.recreate()
                    }
                } catch (e: Exception) {
                    logError(e)
                }
            }
            true
        }

//yeni
        getPref(R.string.pref_filter_search_quality_key)?.setOnPreferenceClickListener {
            val names = enumValues<SearchQuality>().sorted().map { it.name }
            val currentList = settingsManager.getStringSet(
                getString(R.string.pref_filter_search_quality_key),
                setOf()
            )?.map {
                it.toInt()
            } ?: listOf()

            activity?.showMultiDialog(
                names,
                currentList,
                getString(R.string.pref_filter_search_quality),
                {}
            ) { selectedList ->
                settingsManager.edit {
                    putStringSet(
                        getString(R.string.pref_filter_search_quality_key),
                        selectedList.map { it.toString() }.toMutableSet()
                    )
                }
            }

            return@setOnPreferenceClickListener true
        }

        getPref(R.string.tv_layout_clock_key)?.hideOn(PHONE or EMULATOR)

        getPref(R.string.confirm_exit_key)?.setOnPreferenceClickListener {
            val prefNames = resources.getStringArray(R.array.confirm_exit)
            val prefValues = resources.getIntArray(R.array.confirm_exit_values)
            val confirmExit = settingsManager.getInt(getString(R.string.confirm_exit_key), -1)

            activity?.showBottomDialog(
                items = prefNames.toList(),
                selectedIndex = prefValues.indexOf(confirmExit),
                name = getString(R.string.confirm_before_exiting_title),
                showApply = true,
                dismissCallback = {},
                callback = { selectedOption ->
                    settingsManager.edit {
                        putInt(getString(R.string.confirm_exit_key), prefValues[selectedOption])
                    }
                }
            )
            return@setOnPreferenceClickListener true
        }
    }
}
