package com.lagradost.cloudstream3.ui.result

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.databinding.ActorFilmographyBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.BaseBottomSheetDialogFragment
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.discover.DiscoverLanguage
import com.lagradost.cloudstream3.ui.discover.DiscoverSort
import com.lagradost.cloudstream3.ui.discover.TmdbRatingFilter
import com.lagradost.cloudstream3.ui.discover.YearRange
import com.lagradost.cloudstream3.ui.quicksearch.QuickSearchFragment
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_PLAY_FILE
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_SHOW_METADATA
import com.lagradost.cloudstream3.ui.search.SearchAdapter
import com.lagradost.cloudstream3.ui.setRecycledViewPool
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.getSpanCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ActorFilmography : BaseBottomSheetDialogFragment<ActorFilmographyBinding>(
    BaseFragment.BindingCreator.Inflate(ActorFilmographyBinding::inflate)
) {
    companion object {
        private const val TAG = "actor_filmography"
        private const val ACTOR_NAME = "actor_name"
        private const val ACTOR_IMAGE = "actor_image"

        fun show(context: Context, actor: Actor) {
            val manager = (context.getActivity() as? FragmentActivity)?.supportFragmentManager
                ?: return
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return

            ActorFilmography().apply {
                arguments = Bundle().apply {
                    putString(ACTOR_NAME, actor.name)
                    putString(ACTOR_IMAGE, actor.image)
                }
            }.showNow(manager, TAG)
        }
    }

    private enum class FilmographyFilter {
        ALL,
        MOVIES,
        SERIES,
    }

    private var loadJob: Job? = null
    private val repository = ActorFilmographyRepository()
    private var allCredits: List<SearchResponse> = emptyList()
    private var activeFilter = FilmographyFilter.ALL
    private var languageFilter = DiscoverLanguage.ALL
    private var ratingFilter = TmdbRatingFilter.SEVEN
    private var selectedGenres: Set<String> = emptySet()
    private var yearFrom: Int? = null
    private var yearTo: Int? = null
    private var sortFilter = DiscoverSort.POPULAR
    private var availableGenres: List<String> = emptyList()
    private var hasLoaded = false

    override fun onStart() {
        super.onStart()
        view?.let { fixLayout(it) }
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            isFitToContents = false
            expandedOffset = 0
            isDraggable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding?.filmographyResults?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isNestedScrollingEnabled = true
        }
    }

    private fun configureFilmographyGrid(context: Context) {
        val results = binding?.filmographyResults ?: return
        val columns = context.getSpanCount()
        results.spanCount = columns
        val manager = results.layoutManager as? GridLayoutManager
        if (manager == null || manager::class != GridLayoutManager::class) {
            results.layoutManager = GridLayoutManager(context, columns)
        } else {
            manager.spanCount = columns
        }
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(view)
        view.layoutParams?.let {
            it.height = resources.displayMetrics.heightPixels
            view.layoutParams = it
        }
        configureFilmographyGrid(view.context)
    }

    override fun onBindingCreated(binding: ActorFilmographyBinding, savedInstanceState: Bundle?) {
        activeFilter = FilmographyFilter.entries.firstOrNull {
            it.name == savedInstanceState?.getString("filmography_type")
        } ?: activeFilter
        languageFilter = DiscoverLanguage.entries.firstOrNull {
            it.name == savedInstanceState?.getString("filmography_language")
        } ?: languageFilter
        ratingFilter = TmdbRatingFilter.entries.firstOrNull {
            it.name == savedInstanceState?.getString("filmography_rating")
        } ?: ratingFilter
        selectedGenres = savedInstanceState?.getStringArray("filmography_genres")?.toSet().orEmpty()
        yearFrom = savedInstanceState?.getInt("filmography_yearFrom")?.takeIf { it > 0 }
        yearTo = savedInstanceState?.getInt("filmography_yearTo")?.takeIf { it > 0 }

        savedInstanceState?.getInt("filmography_year")?.takeIf { it > 0 }?.let {
            if (yearFrom == null && yearTo == null) { yearFrom = it; yearTo = it }
        }
        sortFilter = DiscoverSort.entries.firstOrNull {
            it.name == savedInstanceState?.getString("filmography_sort")
        } ?: sortFilter

        binding.filmographyActor.text = arguments?.getString(ACTOR_NAME)
        binding.filmographyClose.setOnClickListener { dismiss() }
        binding.filmographyResults.apply {
            configureFilmographyGrid(context)
            setRecycledViewPool(SearchAdapter.sharedPool)
            adapter = SearchAdapter(this) { callback ->
                when (callback.action) {
                    SEARCH_ACTION_LOAD,
                    SEARCH_ACTION_SHOW_METADATA,
                    SEARCH_ACTION_PLAY_FILE -> {
                        QuickSearchFragment.pushSearch(activity, callback.card.name)
                        dismiss()
                    }
                }
            }
        }
        binding.filmographyFilterType.setOnClickListener { showTypeDialog() }
        binding.filmographyFilterLanguage.setOnClickListener { showLanguageDialog() }
        binding.filmographyFilterRating.setOnClickListener { showRatingDialog() }
        binding.filmographyFilterGenres.setOnClickListener { showGenresDialog() }
        binding.filmographyFilterYear.setOnClickListener { showYearDialog() }
        binding.filmographyFilterSort.setOnClickListener { showSortDialog() }
        binding.filmographyFilterReset.setOnClickListener { resetFilters() }
        binding.filmographyRetry.setOnClickListener { loadFilmography() }
        updateChipBar()
        loadFilmography()
    }

    private fun setChip(chip: Chip, filterName: String, value: String) {
        chip.text = getString(R.string.discover_dropdown_value, value)
        chip.contentDescription = "$filterName: $value"
    }

    private fun showTypeDialog() {
        val options = FilmographyFilter.entries.toList()
        val names = options.map {
            when (it) {
                FilmographyFilter.ALL -> getString(R.string.discover_all_types)
                FilmographyFilter.MOVIES -> getString(R.string.discover_movies)
                FilmographyFilter.SERIES -> getString(R.string.discover_series)
            }
        }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_filter_type)
            .setSingleChoiceItems(names, options.indexOf(activeFilter)) { dialog, which ->
                activeFilter = options[which]
                dialog.dismiss()
                updateChipBar()
                applyFilter()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val options = DiscoverLanguage.entries.toList()
        val names = options.map { getString(it.labelRes) }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_filter_language)
            .setSingleChoiceItems(names, options.indexOf(languageFilter)) { dialog, which ->
                languageFilter = options[which]
                dialog.dismiss()
                updateChipBar()
                applyFilter()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun ratingName(rating: TmdbRatingFilter): String =
        if (rating == TmdbRatingFilter.ALL) getString(R.string.discover_all)
        else "${rating.minimum}+"

    private fun showRatingDialog() {
        val options = TmdbRatingFilter.entries.sortedBy { it.minimum }
        val names = options.map { ratingName(it) }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_filter_rating)
            .setSingleChoiceItems(names, options.indexOf(ratingFilter)) { dialog, which ->
                ratingFilter = options[which]
                dialog.dismiss()
                updateChipBar()
                applyFilter()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showGenresDialog() {
        if (availableGenres.isEmpty()) return
        val options = availableGenres
        val pending = selectedGenres.toMutableSet()
        val checked = options.map { it in pending }.toBooleanArray()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_filter_genres)
            .setMultiChoiceItems(options.toTypedArray(), checked) { _, which, isChecked ->
                if (isChecked) pending.add(options[which]) else pending.remove(options[which])
            }
            .setPositiveButton(R.string.discover_apply) { _, _ ->
                selectedGenres = pending
                updateChipBar()
                applyFilter()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.discover_clear, null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            pending.clear()
            for (i in options.indices) dialog.listView.setItemChecked(i, false)
        }
    }

    private fun yearRangeLabel(range: YearRange): String {
        if (range.isAll) return getString(R.string.discover_all)
        if (range.from != null && range.to != null && range.to == range.from + 9 && range.from % 10 == 0) {
            return if (range.from >= 2000) "${range.from}s" else "${range.from % 100}s"
        }
        return range.label()
    }

    private fun yearPresets(): List<YearRange> {
        val y = Calendar.getInstance().get(Calendar.YEAR)
        return listOf(
            YearRange(null, null),
            YearRange(y, y),
            YearRange(y - 1, y - 1),
            YearRange(2020, 2029),
            YearRange(2015, 2019),
            YearRange(2010, 2019),
            YearRange(2000, 2009),
            YearRange(1990, 1999),
            YearRange(1980, 1989),
            YearRange(1970, 1979),
        )
    }

    private fun showYearDialog() {
        val presets = yearPresets()
        val current = YearRange(yearFrom, yearTo)
        val labels = presets.map { yearRangeLabel(it) }.toMutableList()
        labels.add(getString(R.string.discover_year_custom))
        val checked = presets.indexOf(current).takeIf { it >= 0 } ?: -1
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_filter_year)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                if (which == presets.size) {
                    dialog.dismiss()
                    showCustomYearRangeDialog()
                } else {
                    val r = presets[which]
                    yearFrom = r.from; yearTo = r.to
                    dialog.dismiss()
                    updateChipBar()
                    applyFilter()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCustomYearRangeDialog() {
        val years: List<Int?> = listOf(null) + (Calendar.getInstance().get(Calendar.YEAR) downTo 1960).toList()
        val names = years.map { it?.toString() ?: getString(R.string.discover_all) }.toTypedArray()
        var pendingFrom = yearFrom
        var pendingTo = yearTo
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_year_from)
            .setSingleChoiceItems(names, years.indexOf(pendingFrom).takeIf { it >= 0 } ?: 0) { dFrom, whichFrom ->
                pendingFrom = years[whichFrom]
                dFrom.dismiss()
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.discover_year_to)
                    .setSingleChoiceItems(names, years.indexOf(pendingTo).takeIf { it >= 0 } ?: 0) { dTo, whichTo ->
                        pendingTo = years[whichTo]
                        dTo.dismiss()
                        var f = pendingFrom; var t = pendingTo
                        if (f != null && t != null && f > t) { val tmp = f; f = t; t = tmp }
                        yearFrom = f; yearTo = t
                        updateChipBar()
                        applyFilter()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSortDialog() {
        val options = DiscoverSort.entries.toList()
        val names = options.map { getString(it.labelRes) }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discover_filter_sort)
            .setSingleChoiceItems(names, options.indexOf(sortFilter)) { dialog, which ->
                sortFilter = options[which]
                dialog.dismiss()
                updateChipBar()
                applyFilter()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun isDefault(): Boolean =
        activeFilter == FilmographyFilter.ALL &&
            languageFilter == DiscoverLanguage.ALL &&
            ratingFilter == TmdbRatingFilter.SEVEN &&
            selectedGenres.isEmpty() &&
            yearFrom == null && yearTo == null &&
            sortFilter == DiscoverSort.POPULAR

    private fun resetFilters() {
        if (isDefault()) return
        activeFilter = FilmographyFilter.ALL
        languageFilter = DiscoverLanguage.ALL
        ratingFilter = TmdbRatingFilter.SEVEN
        selectedGenres = emptySet()
        yearFrom = null; yearTo = null
        sortFilter = DiscoverSort.POPULAR
        updateChipBar()
        applyFilter()
    }

    private fun updateChipBar() {
        val binding = binding ?: return
        val typeName = when (activeFilter) {
            FilmographyFilter.ALL -> getString(R.string.discover_all_types)
            FilmographyFilter.MOVIES -> getString(R.string.discover_movies)
            FilmographyFilter.SERIES -> getString(R.string.discover_series)
        }
        setChip(binding.filmographyFilterType, getString(R.string.discover_filter_type), typeName)
        setChip(binding.filmographyFilterLanguage, getString(R.string.discover_filter_language), getString(languageFilter.labelRes))
        setChip(binding.filmographyFilterRating, getString(R.string.discover_filter_rating), ratingName(ratingFilter))
        val genreValue = if (selectedGenres.isEmpty()) getString(R.string.discover_filter_genres)
        else getString(R.string.discover_genres_selected, selectedGenres.size)
        setChip(binding.filmographyFilterGenres, getString(R.string.discover_filter_genres), genreValue)

        binding.filmographyFilterGenres.isEnabled = true

        val yearLabel = yearRangeLabel(YearRange(yearFrom, yearTo))
        setChip(binding.filmographyFilterYear, getString(R.string.discover_filter_year), yearLabel)
        setChip(binding.filmographyFilterSort, getString(R.string.discover_filter_sort), getString(sortFilter.labelRes))
        binding.filmographyFilterReset.isVisible = !isDefault()
    }

    private fun SearchResponse.getTagsList(): List<String> {
        // TmdbMetadata üzerinden posterHeaders["X-Tags"] içerisine yazılan virgülle ayrılmış kategorileri ayrıştırır
        return runCatching {
            this.posterHeaders?.get("X-Tags")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        }.getOrNull().orEmpty()
    }

    private fun SearchResponse.getOriginalLanguage(): String? {
        // TmdbMetadata üzerinden posterHeaders["Accept-Language"] içerisine yazılan dil kodunu okur
        return runCatching {
            this.posterHeaders?.get("Accept-Language")
        }.getOrNull()
    }

    private fun SearchResponse.getReleaseYear(): Int? {
        return runCatching {
            var foundYear: Int? = null
            var clazz: Class<*>? = this::class.java
            while (clazz != null && clazz != Any::class.java) {
                for (field in clazz.declaredFields) {
                    if (field.name == "year" || field.name == "releaseDate") {
                        field.isAccessible = true
                        when (val value = field.get(this)) {
                            is Int -> foundYear = value
                            is String -> foundYear = value.take(4).toIntOrNull()
                        }
                    }
                    if (foundYear != null) break
                }
                if (foundYear != null) break
                clazz = clazz.superclass
            }
            foundYear
        }.getOrNull()
    }

    private fun applyFilter() {
        val binding = binding ?: return
        if (!hasLoaded) return
        val yf = yearFrom
        val yt = yearTo

        var filtered = when (activeFilter) {
            FilmographyFilter.ALL -> allCredits
            FilmographyFilter.MOVIES -> allCredits.filter { it.type == TvType.Movie }
            FilmographyFilter.SERIES -> allCredits.filter { it.type == TvType.TvSeries }
        }.filter { ratingFilter.matches(it.score) }
            .filter {
                val filterCode = languageFilter.code?.lowercase()
                if (filterCode == null) true
                else {
                    val itemLang = it.getOriginalLanguage()?.lowercase()
                    itemLang != null && (itemLang == filterCode || itemLang.startsWith(filterCode))
                }
            }
            .filter { yf == null || (it.getReleaseYear()?.let { y -> y >= yf } == true) }
            .filter { yt == null || (it.getReleaseYear()?.let { y -> y <= yt } == true) }
            .filter { item ->
                if (selectedGenres.isEmpty()) true
                else {
                    item.getTagsList().any { tag ->
                        selectedGenres.any { selected -> tag.equals(selected, ignoreCase = true) }
                    }
                }
            }

        filtered = when (sortFilter) {
            DiscoverSort.NEWEST -> filtered.sortedWith(
                compareByDescending<SearchResponse> { it.getReleaseYear() ?: 0 }.thenBy { it.name }
            )
            DiscoverSort.OLDEST -> filtered.sortedWith(
                compareBy<SearchResponse> { it.getReleaseYear() ?: Int.MAX_VALUE }.thenBy { it.name }
            )
            DiscoverSort.TOP_RATED -> filtered.sortedWith(
                compareByDescending<SearchResponse> { it.score?.toDouble() ?: -1.0 }.thenBy { it.name }
            )
            DiscoverSort.TITLE_AZ -> filtered.sortedBy { it.name.lowercase() }
            else -> filtered
        }

        (binding.filmographyResults.adapter as? SearchAdapter)?.submitList(filtered)
        binding.filmographyResults.isVisible = filtered.isNotEmpty()
        binding.filmographyStatus.setText(
            if (allCredits.isEmpty()) R.string.actor_filmography_empty
            else R.string.actor_filmography_no_matches
        )
        binding.filmographyStatus.isVisible = filtered.isEmpty() && !binding.filmographyLoading.isVisible
        updateChipBar()
    }

    private fun loadFilmography() {
        val binding = binding ?: return
        val actor = Actor(
            name = arguments?.getString(ACTOR_NAME).orEmpty(),
            image = arguments?.getString(ACTOR_IMAGE),
        )
        loadJob?.cancel()
        hasLoaded = false
        allCredits = emptyList()
        availableGenres = emptyList()
        binding.filmographyLoading.isVisible = true
        binding.filmographyStatus.isVisible = false
        binding.filmographyRetry.isVisible = false
        binding.filmographyResults.isVisible = false
        updateChipBar()

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val details = repository.details(actor)
                    val bio = details?.biography?.trim()

                    val formattedBio = if (!bio.isNullOrEmpty()) {
                        bio
                    } else {
                        val infoList = mutableListOf<String>()

                        details?.department?.trim()?.takeIf { it.isNotEmpty() }?.let { dept ->
                            infoList.add("Meslek: $dept")
                        }

                        details?.birthplace?.trim()?.takeIf { it.isNotEmpty() }?.let { place ->
                            infoList.add("Doğum Yeri: $place")
                        }

                        details?.birthday?.trim()?.takeIf { it.isNotEmpty() }?.let { bday ->
                            val formattedBday = formatDate(bday)
                            val age = details.age()
                            if (age != null) {
                                val ageText = if (details.deathday.isNullOrBlank()) "$age yaşında" else "$age yaşında vefat etti"
                                infoList.add("Doğum Tarihi: $formattedBday ($ageText)")
                            } else {
                                infoList.add("Doğum Tarihi: $formattedBday")
                            }
                        }

                        details?.deathday?.trim()?.takeIf { it.isNotEmpty() }?.let { dday ->
                            infoList.add("Ölüm Tarihi: ${formatDate(dday)}")
                        }

                        if (infoList.isNotEmpty()) {
                            infoList.joinToString(" • ")
                        } else {
                            null
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (!formattedBio.isNullOrEmpty()) {
                            binding.filmographyBio.text = formattedBio
                            binding.filmographyBio.isVisible = true
                        } else {
                            binding.filmographyBio.isVisible = false
                        }

                        val imageUrl = actor.image
                        if (!imageUrl.isNullOrEmpty()) {
                            binding.filmographyActorImage.loadImage(imageUrl)
                            binding.filmographyActorImage.isVisible = true
                        } else {
                            binding.filmographyActorImage.isVisible = false
                        }
                    }
                }

                val credits = withContext(Dispatchers.IO) { repository.load(actor) }
                allCredits = credits

                val fetchedGenres = credits.flatMap { it.getTagsList() }.filter { it.isNotBlank() }.distinct().sorted()

                availableGenres = fetchedGenres
                selectedGenres = selectedGenres.filter { selected -> 
                    availableGenres.any { it.equals(selected, ignoreCase = true) } 
                }.toSet()

                hasLoaded = true
                binding.filmographyLoading.isVisible = false
                applyFilter()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logError(error)
                binding.filmographyStatus.setText(R.string.actor_filmography_error)
                binding.filmographyStatus.isVisible = true
                binding.filmographyRetry.isVisible = true
            } finally {
                if (isActive) binding.filmographyLoading.isVisible = false
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        loadJob?.cancel()
        super.onDismiss(dialog)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("filmography_type", activeFilter.name)
        outState.putString("filmography_language", languageFilter.name)
        outState.putString("filmography_rating", ratingFilter.name)
        outState.putStringArray("filmography_genres", selectedGenres.toTypedArray())
        yearFrom?.let { outState.putInt("filmography_yearFrom", it) }
        yearTo?.let { outState.putInt("filmography_yearTo", it) }
        outState.putString("filmography_sort", sortFilter.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        loadJob = null
        allCredits = emptyList()
        availableGenres = emptyList()
        hasLoaded = false
        binding?.filmographyResults?.adapter = null
        super.onDestroyView()
    }

    private fun formatDate(dateStr: String): String {
        return runCatching {
            val parts = dateStr.split("-")
            if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else dateStr
        }.getOrDefault(dateStr)
    }
}