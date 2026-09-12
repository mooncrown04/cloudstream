package com.lagradost.cloudstream3

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.View.NO_ID
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isNotEmpty
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.CastSession
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigationrail.NavigationRailView
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.removeKey
import com.lagradost.cloudstream3.actions.OpenInAppAction
import com.lagradost.cloudstream3.actions.VideoClickActionHolder
import com.lagradost.cloudstream3.databinding.ToastBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.ui.home.HomeChildItemAdapter
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.player.PlayerPipHelper.isPIPPossible
import com.lagradost.cloudstream3.ui.player.Torrent
import com.lagradost.cloudstream3.ui.result.ActorAdaptor
import com.lagradost.cloudstream3.ui.result.EpisodeAdapter
import com.lagradost.cloudstream3.ui.result.ImageAdapter
import com.lagradost.cloudstream3.ui.search.SearchAdapter
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.updateTv
import com.lagradost.cloudstream3.ui.settings.extensions.PluginAdapter
import com.lagradost.cloudstream3.utils.AppContextUtils.isRtl
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Event
import com.lagradost.cloudstream3.utils.UIHelper.showInputMethod
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream3.utils.UiText
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import org.schabi.newpipe.extractor.NewPipe

enum class FocusDirection {
    Start,
    End,
    Up,
    Down,
}

object CommonActivity {

    private var _activity: WeakReference<Activity>? = null
    var activity
        get() = _activity?.get()
        private set(value) {
            _activity = WeakReference(value)
        }

    @MainThread
    fun setActivityInstance(newActivity: Activity?) {
        activity = newActivity
    }

    @MainThread
    fun Activity?.getCastSession(): CastSession? {
        return (this as MainActivity?)?.mSessionManager?.currentCastSession
    }

    val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics

    val screenWidth: Int
        get() {
            return max(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    val screenHeight: Int
        get() {
            return min(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    val screenWidthWithOrientation: Int
        get() {
            return displayMetrics.widthPixels
        }
    val screenHeightWithOrientation: Int
        get() {
            return displayMetrics.heightPixels
        }

    var isPipDesired: Boolean = false
    var isInPIPMode: Boolean = false

    val onColorSelectedEvent = Event<Pair<Int, Int>>()
    val onDialogDismissedEvent = Event<Int>()

    var keyEventListener: ((Pair<KeyEvent?, Boolean>) -> Boolean)? = null
    var appliedTheme: Int = 0
    var appliedColor: Int = 0

    private var currentToast: Toast? = null

    fun showToast(@StringRes message: Int, duration: Int? = null) {
        val act = activity ?: return
        act.runOnUiThread {
            showToast(act, act.getString(message), duration)
        }
    }

    fun showToast(message: String?, duration: Int? = null) {
        val act = activity ?: return
        act.runOnUiThread {
            showToast(act, message, duration)
        }
    }

    fun showToast(message: UiText?, duration: Int? = null) {
        val act = activity ?: return
        if (message == null) return
        act.runOnUiThread {
            showToast(act, message.asString(act), duration)
        }
    }

    @MainThread
    fun showToast(act: Activity?, text: UiText, duration: Int) {
        if (act == null) return
        text.asStringNull(act)?.let {
            showToast(act, it, duration)
        }
    }

    @MainThread
    fun showToast(act: Activity?, @StringRes message: Int, duration: Int? = null) {
        if (act == null) return
        showToast(act, act.getString(message), duration)
    }

    const val TAG = "COMPACT"

    @MainThread
    fun showToast(act: Activity?, message: String?, duration: Int? = null) {
        if (act == null || message == null) {
            Log.w(TAG, "invalid showToast act = $act message = $message")
            return
        }
        Log.i(TAG, "showToast = $message")

        try {
            currentToast?.cancel()
        } catch (e: Exception) {
            logError(e)
        }

        try {
            val binding = ToastBinding.inflate(act.layoutInflater)
            binding.text.text = message.trim()

            val toast = Toast(act)
            toast.duration = duration ?: Toast.LENGTH_SHORT
            toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 5.toPx)
            @Suppress("DEPRECATION")
            toast.view = binding.root
            currentToast = toast
            toast.show()

            val handler = Handler(Looper.getMainLooper())
            val ref = WeakReference(toast)

            handler.postDelayed({
                if (ref.get() == currentToast) {
                    currentToast = null
                }
            }, 10_000)

        } catch (e: Exception) {
            logError(e)
        }
    }

    fun setLocale(context: Context?, languageTag: String?) {
        if (context == null || languageTag == null) return
        val locale = Locale.forLanguageTag(languageTag)
        val resources: Resources = context.resources
        val config = resources.configuration
        Locale.setDefault(locale)
        config.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.createConfigurationContext(config)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(
            config,
            resources.displayMetrics
        )
    }

    fun Context.updateLocale() {
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        val localeCode = settingsManager.getString(getString(R.string.locale_key), null)
        setLocale(this, localeCode)
    }

    fun init(act: Activity) {
        setActivityInstance(act)
        ioSafe { Torrent.deleteAllFiles() }
        val componentActivity = activity as? ComponentActivity ?: return

        componentActivity.updateLocale()
        componentActivity.updateTv()
        AccountManager.initMainAPI()
        NewPipe.init(DownloaderTestImpl.getInstance())

        MainActivity.activityResultLauncher =
            componentActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val actionUid =
                        getKey<String>("last_click_action") ?: return@registerForActivityResult
                    Log.d(TAG, "Loading action $actionUid result handler")
                    val action = VideoClickActionHolder.getByUniqueId(actionUid) as? OpenInAppAction
                        ?: return@registerForActivityResult
                    action.onResultSafe(act, result.data)
                    removeKey("last_click_action")
                    removeKey("last_opened")
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                componentActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val requestPermissionLauncher = componentActivity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                Log.d(TAG, "Notification permission: $isGranted")
            }
            requestPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun Activity.enterPIPMode() {
        if (!isPipDesired || !this.isPIPPossible()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                } catch (_: Exception) {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun onUserLeaveHint(act: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        act.enterPIPMode()
    }

    fun updateTheme(act: Activity) {
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(act)
        if (settingsManager
                .getString(act.getString(R.string.app_theme_key), "AmoledLight") == "System"
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            loadThemes(act)
        }
    }

    private fun mapSystemTheme(act: Activity): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val currentNightMode =
                act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return when (currentNightMode) {
                Configuration.UI_MODE_NIGHT_NO -> R.style.LightMode
                else -> R.style.AppTheme
            }
        } else {
            return R.style.AppTheme
        }
    }

    fun loadThemes(act: Activity?) {
        if (act == null) return
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(act)

        val currentTheme =
            when (settingsManager.getString(act.getString(R.string.app_theme_key), "AmoledLight")) {
                "System" -> mapSystemTheme(act)
                "Black" -> R.style.AppTheme
                "Light" -> R.style.LightMode
                "Amoled" -> R.style.AmoledMode
                "AmoledLight" -> R.style.AmoledModeLight
                "Monet" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    R.style.MonetMode else R.style.AppTheme

                "Dracula" -> R.style.DraculaMode
                "Lavender" -> R.style.LavenderMode
                "SilentBlue" -> R.style.SilentBlueMode

                else -> R.style.AppTheme
            }

        val currentOverlayTheme =
            when (settingsManager.getString(act.getString(R.string.primary_color_key), "Normal")) {
                "Normal" -> R.style.OverlayPrimaryColorNormal
                "DandelionYellow" -> R.style.OverlayPrimaryColorDandelionYellow
                "CarnationPink" -> R.style.OverlayPrimaryColorCarnationPink
                "Orange" -> R.style.OverlayPrimaryColorOrange
                "DarkGreen" -> R.style.OverlayPrimaryColorDarkGreen
                "Maroon" -> R.style.OverlayPrimaryColorMaroon
                "NavyBlue" -> R.style.OverlayPrimaryColorNavyBlue
                "Grey" -> R.style.OverlayPrimaryColorGrey
                "White" -> R.style.OverlayPrimaryColorWhite
                "CoolBlue" -> R.style.OverlayPrimaryColorCoolBlue
                "Brown" -> R.style.OverlayPrimaryColorBrown
                "Purple" -> R.style.OverlayPrimaryColorPurple
                "Green" -> R.style.OverlayPrimaryColorGreen
                "GreenApple" -> R.style.OverlayPrimaryColorGreenApple
                "Red" -> R.style.OverlayPrimaryColorRed
                "Banana" -> R.style.OverlayPrimaryColorBanana
                "Party" -> R.style.OverlayPrimaryColorParty
                "Pink" -> R.style.OverlayPrimaryColorPink
                "Lavender" -> R.style.OverlayPrimaryColorLavender
                "Monet" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    R.style.OverlayPrimaryColorMonet else R.style.OverlayPrimaryColorNormal

                "Monet2" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    R.style.OverlayPrimaryColorMonetTwo else R.style.OverlayPrimaryColorNormal

                else -> R.style.OverlayPrimaryColorNormal
            }

        // Font Overlay Uygulaması
    val currentFontOverlay = when (settingsManager.getString(act.getString(R.string.app_font_key), "Default")) {
        "ComicSans" -> R.style.ComicSansFontOverlay
        "Gotham" -> R.style.GothamFontOverlay
        "NetflixSans" -> R.style.NetflixSansFontOverlay
        "Ubuntu" -> R.style.UbuntuFontOverlay
        "OpenSans" -> R.style.OpenSansFontOverlay
        else -> null
    }

    act.theme.applyStyle(currentTheme, true)
    act.theme.applyStyle(currentOverlayTheme, true)
    currentFontOverlay?.let { act.theme.applyStyle(it, true) }

    appliedTheme = currentTheme
    appliedColor = currentOverlayTheme
    act.updateTv()
    if (isLayout(TV)) act.theme.applyStyle(R.style.AppThemeTvOverlay, true)
    act.theme.applyStyle(R.style.LoadedStyle, true)
}

    private fun localLook(from: View, id: Int): View? {
        if (id == NO_ID) return null
        var currentLook: View = from
        for (i in 0..15) {
            currentLook.findViewById<View?>(id)?.let { return it }
            currentLook = (currentLook.parent as? View) ?: break
        }
        return null
    }

    private fun View.hasContent(): Boolean {
        return isShown && when (this) {
            is ViewGroup -> this.isNotEmpty()
            else -> true
        }
    }

    fun continueGetNextFocus(
        root: Any?,
        view: View,
        direction: FocusDirection,
        nextId: Int,
        depth: Int = 0
    ): View? {
        if (nextId == NO_ID) return null

        var next =
            when (root) {
                is Activity -> root.findViewById(nextId)
                is View -> root.rootView.findViewById<View?>(nextId)
                else -> null
            } ?: return null

        next = localLook(view, nextId) ?: next
        val shown = next.hasContent()

        val hasChildrenThatWantsFocus = (next as? ViewGroup)?.let { parent ->
            parent.descendantFocusability == ViewGroup.FOCUS_AFTER_DESCENDANTS && parent.isNotEmpty()
        } ?: false
        if (!next.isFocusable && shown && !hasChildrenThatWantsFocus) return null

        if (!shown) {
            if (next == view) return null
            return getNextFocus(root, next, direction, depth + 1)
        }

        (when (next) {
            is ChipGroup -> {
                next.children.firstOrNull { it.isFocusable && it.isShown }
            }

            is NavigationRailView -> {
                next.findViewById(next.selectedItemId) ?: next.findViewById(R.id.navigation_home)
            }

            else -> null
        })?.let {
            return it
        }

        return next
    }

    fun getNextFocus(
        root: Any?,
        view: View?,
        direction: FocusDirection,
        depth: Int = 0
    ): View? {
        if (view == null || depth >= 10 || root == null) {
            return null
        }

        var nextId = when (direction) {
            FocusDirection.Start -> {
                if (view.isRtl())
                    view.nextFocusRightId
                else
                    view.nextFocusLeftId
            }

            FocusDirection.Up -> {
                view.nextFocusUpId
            }

            FocusDirection.End -> {
                if (view.isRtl())
                    view.nextFocusLeftId
                else
                    view.nextFocusRightId
            }

            FocusDirection.Down -> {
                view.nextFocusDownId
            }
        }

        if (nextId == NO_ID) {
            nextId = view.nextFocusForwardId
            if (nextId == NO_ID)
                return null
        }
        return continueGetNextFocus(root, view, direction, nextId, depth)
    }

    fun onKeyDown(act: Activity?, keyCode: Int, event: KeyEvent?): Boolean? {
        return null
    }

    fun dispatchKeyEvent(act: Activity?, event: KeyEvent?): Boolean? {
        if (act == null) return null
        val currentFocus = act.currentFocus

        event?.keyCode?.let { keyCode ->
            if (currentFocus == null || event.action != KeyEvent.ACTION_DOWN) return@let
            val nextView = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> getNextFocus(
                    act,
                    currentFocus,
                    FocusDirection.Start
                )

                KeyEvent.KEYCODE_DPAD_RIGHT -> getNextFocus(
                    act,
                    currentFocus,
                    FocusDirection.End
                )

                KeyEvent.KEYCODE_DPAD_UP -> getNextFocus(
                    act,
                    currentFocus,
                    FocusDirection.Up
                )

                KeyEvent.KEYCODE_DPAD_DOWN -> getNextFocus(
                    act,
                    currentFocus,
                    FocusDirection.Down
                )

                else -> null
            }

            if (nextView != null) {
                nextView.requestFocus()
                keyEventListener?.invoke(Pair(event, true))
                return true
            }

            @SuppressLint("RestrictedApi")
            if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) &&
                (act.currentFocus is SearchView || act.currentFocus is SearchView.SearchAutoComplete)
            ) {
                showInputMethod(act.currentFocus?.findFocus())
            }
        }

        if (keyEventListener?.invoke(Pair(event, false)) == true) {
            return true
        }
        return null
    }
}
