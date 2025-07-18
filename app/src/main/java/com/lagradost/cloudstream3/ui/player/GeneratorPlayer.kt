package com.lagradost.cloudstream3.ui.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.format.DateUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerNotificationManager.EXTRA_INSTANCE_ID
import androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.button.MaterialButton
import com.lagradost.cloudstream3.APIHolder.getApiFromNameNull
import com.lagradost.cloudstream3.AcraApplication
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity.keyEventListener
import com.lagradost.cloudstream3.CommonActivity.playerEventListener
import com.lagradost.cloudstream3.CommonActivity.screenHeight
import com.lagradost.cloudstream3.CommonActivity.screenWidth
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.getAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.getImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.getMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.getTMDbId
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.databinding.DialogOnlineSubtitlesBinding
import com.lagradost.cloudstream3.databinding.FragmentPlayerBinding
import com.lagradost.cloudstream3.databinding.PlayerCustomLayoutBinding
import com.lagradost.cloudstream3.databinding.SubtitleOffsetBinding
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.observeNullable
import com.lagradost.cloudstream3.subtitles.AbstractSubApi
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.subtitles.SubRepository
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import com.lagradost.cloudstream3.ui.player.CS3IPlayer.Companion.preferredAudioTrackLanguage
import com.lagradost.cloudstream3.ui.player.CustomDecoder.Companion.updateForcedEncoding
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.Companion.toSubtitleMimeType
import com.lagradost.cloudstream3.ui.player.source_priority.QualityDataHelper
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.SyncViewModel
import com.lagradost.cloudstream3.ui.subtitles.SUBTITLE_AUTO_SELECT_KEY
import com.lagradost.cloudstream3.utils.AppContextUtils.html
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.runOnMainThread
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showDialog
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTwoLettersToLanguage
import com.lagradost.cloudstream3.utils.SubtitleHelper.languages
import com.lagradost.cloudstream3.utils.UIHelper.colorFromAttribute
import com.lagradost.cloudstream3.utils.UIHelper.dismissSafe
import com.lagradost.cloudstream3.utils.UIHelper.hideSystemUI
import com.lagradost.cloudstream3.utils.UIHelper.popCurrentPage
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream3.utils.VideoDownloadManager.getImageBitmapFromUrl
import com.lagradost.cloudstream3.utils.setText
import com.lagradost.cloudstream3.utils.txt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.Calendar
import kotlin.math.abs
import androidx.core.content.edit


const val MINIMUM_SEEK_TIME = 7000L         // when swipe seeking
const val MINIMUM_VERTICAL_SWIPE = 2.0f     // in percentage
const val MINIMUM_HORIZONTAL_SWIPE = 2.0f   // in percentage
const val VERTICAL_MULTIPLIER = 2.0f
const val HORIZONTAL_MULTIPLIER = 2.0f
const val DOUBLE_TAB_MAXIMUM_HOLD_TIME = 200L
const val DOUBLE_TAB_MINIMUM_TIME_BETWEEN = 200L    // this also affects the UI show response time
const val DOUBLE_TAB_PAUSE_PERCENTAGE = 0.15        // in both directions
private const val SUBTITLE_DELAY_BUNDLE_KEY = "subtitle_delay"

// Interface to communicate channel switching requests to the parent (e.g., GeneratorPlayer)
interface ChannelSwitchListener {
    fun onNextChannelRequested()
    fun onPreviousChannelRequested()
    fun onChannelSelected(index: Int)
    // Optional: If you need to get the list of channels from the parent for a dialog
    // fun getAvailableChannels(): List<MovieSearchResponse> // Or whatever type your channels are
}

// All the UI Logic for the player
open class FullScreenPlayer : AbstractPlayerFragment() {
    private var isVerticalOrientation: Boolean = false
    protected open var lockRotation = true
    protected open var isFullScreenPlayer = true
    protected var playerBinding: PlayerCustomLayoutBinding? = null

    private var durationMode: Boolean by UserPreferenceDelegate("duration_mode", false)

    // state of player UI
    protected var isShowing = false
    protected var isLocked = false

    protected var hasEpisodes = false
        private set
    //protected val hasEpisodes
    //    get() = episodes.isNotEmpty()

    // options for player

    /**
     * Default profile 1
     * Decides how links should be sorted based on a priority system.
     * This will be set in runtime based on settings.
     **/
    protected var currentQualityProfile = 1

    //    protected var currentPrefQuality =
//        Qualities.P2160.value // preferred maximum quality, used for ppl w bad internet or on cell
    protected var fastForwardTime = 10000L
    protected var androidTVInterfaceOffSeekTime = 10000L
    protected var androidTVInterfaceOnSeekTime = 30000L
    protected var swipeHorizontalEnabled = false
    protected var swipeVerticalEnabled = false
    protected var playBackSpeedEnabled = false
    protected var playerResizeEnabled = false
    protected var doubleTapEnabled = false
    protected var doubleTapPauseEnabled = true
    protected var playerRotateEnabled = false
    protected var autoPlayerRotateEnabled = false
    private var hideControlsNames = false
    protected var speedupEnabled = false
    protected var subtitleDelay
        set(value) = try {
            player.setSubtitleOffset(-value)
        } catch (e: Exception) {
            logError(e)
        }
        get() = try {
            -player.getSubtitleOffset()
        } catch (e: Exception) {
            logError(e)
            0L
        }

    //private var useSystemBrightness = false
    protected var useTrueSystemBrightness = true
    private val fullscreenNotch = true //TODO SETTING

    private var statusBarHeight: Int? = null
    private var navigationBarHeight: Int? = null

    private val brightnessIcons = listOf(
        R.drawable.sun_1,
        R.drawable.sun_2,
        R.drawable.sun_3,
        R.drawable.sun_4,
        R.drawable.sun_5,
        R.drawable.sun_6,
        //R.drawable.sun_7,
        // R.drawable.ic_baseline_brightness_1_24,
        // R.drawable.ic_baseline_brightness_2_24,
        // R.drawable.ic_baseline_brightness_3_24,
        // R.drawable.ic_baseline_brightness_4_24,
        // R.drawable.ic_baseline_brightness_5_24,
        // R.drawable.ic_baseline_brightness_6_24,
        // R.drawable.ic_baseline_brightness_7_24,
    )

    private val volumeIcons = listOf(
        R.drawable.ic_baseline_volume_mute_24,
        R.drawable.ic_baseline_volume_down_24,
        R.drawable.ic_baseline_volume_up_24,
    )

    // Listener for channel switching, to be set by the parent Fragment/Activity
    protected var channelSwitchListener: ChannelSwitchListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState) ?: return null
        playerBinding = PlayerCustomLayoutBinding.bind(root.findViewById(R.id.player_holder))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the channelSwitchListener if the parent implements it
        channelSwitchListener = activity as? ChannelSwitchListener ?: parentFragment as? ChannelSwitchListener

        playerBinding?.apply {
            // Set up click listeners for the new channel buttons
            playerPrevChannelBtt.setOnClickListener {
                channelSwitchListener?.onPreviousChannelRequested()
                showToast(activity, getString(R.string.player_prev_channel_loading)) // Provide user feedback
                onClickChange() // Hide controls after click
            }

            playerNextChannelBtt.setOnClickListener {
                channelSwitchListener?.onNextChannelRequested()
                showToast(activity, getString(R.string.player_next_channel_loading)) // Provide user feedback
                onClickChange() // Hide controls after click
            }

            // Existing button listeners, ensure they are correctly bound to the new XML structure
            playerGoBack.setOnClickListener {
                activity?.popCurrentPage()
            }

            playerLock.setOnClickListener {
                toggleLock()
            }

            playerSourcesBtt.setOnClickListener {
                showMirrorsDialogue()
            }

            playerSubtitleOffsetBtt.setOnClickListener {
                showSubtitleOffsetDialog()
            }

            playerSpeedBtt.setOnClickListener {
                showSpeedDialog()
            }

            playerRew.setOnClickListener {
                rewind()
            }

            playerFfwd.setOnClickListener {
                fastForward()
            }

            playerPausePlay.setOnClickListener {
                player.handleEvent(CSPlayerEvent.PlayPause, PlayerEventSource.UI)
            }

            skipChapterButton.setOnClickListener {
                skipOp()
            }

            // Add other button listeners as needed based on your full XML layout
            // For example, if you have exo_prev, exo_next, etc. from the commented out section
            // exo_prev.setOnClickListener { player.seekToPrevious() }
            // exo_next.setOnClickListener { player.seekToNext() }
        }
    }


    override fun onDestroyView() {
        playerBinding = null
        super.onDestroyView()
    }

    open fun showMirrorsDialogue() {
        throw NotImplementedError()
    }

    open fun showTracksDialogue() {
        throw NotImplementedError()
    }

    open fun openOnlineSubPicker(
        context: Context,
        loadResponse: LoadResponse?,
        dismissCallback: (() -> Unit)
    ) {
        throw NotImplementedError()
    }

    /**
     * [isValidTouch] should be called on a [View] spanning across the screen for reliable results.
     *
     * Android has supported gesture navigation properly since API-30. We get the absolute screen dimens using
     * [WindowManager.getCurrentWindowMetrics] and remove the stable insets
     * {[WindowInsets.getInsetsIgnoringVisibility]} to get a safe perimeter.
     * This approach supports any and all types of necessary system insets.
     *
     * @return false if the touch is on the status bar or navigation bar
     * */
    private fun View.isValidTouch(rawX: Float, rawY: Float): Boolean {
        // NOTE: screenWidth is without the navbar width when 3button nav is turned on.
        if (Build.VERSION.SDK_INT >= 30) {
            // real = absolute dimen without any default deductions like navbar width
            val windowMetrics =
                (context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.currentWindowMetrics
            val realScreenHeight =
                windowMetrics?.let { windowMetrics.bounds.bottom - windowMetrics.bounds.top }
                    ?: screenHeight
            val realScreenWidth =
                windowMetrics?.let { windowMetrics.bounds.right - windowMetrics.bounds.left }
                    ?: screenWidth

            val insets =
                rootWindowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val isOutsideHeight = rawY < insets.top || rawY > (realScreenHeight - insets.bottom)
            val isOutsideWidth = if (windowMetrics == null) {
                rawX < screenWidth
            } else rawX < insets.left || rawX > realScreenWidth - insets.right

            return !(isOutsideWidth || isOutsideHeight)
        } else {
            val statusHeight = statusBarHeight ?: 0
            return rawY > statusHeight && rawX < screenWidth
        }
    }

    override fun exitedPipMode() {
        animateLayoutChanges()
    }

    private fun animateLayoutChangesForSubtitles() =
        // Post here as bottomPlayerBar is gone the first frame => bottomPlayerBar.height = 0
        playerBinding?.bottomPlayerBar?.post {
            @OptIn(UnstableApi::class)
            val sView = subView ?: return@post // subView is assumed to be defined in AbstractPlayerFragment
            val sStyle = CustomDecoder.style // CustomDecoder is assumed to be an existing class
            val binding = playerBinding ?: return@post

            val move = if (isShowing) minOf(
                // We do not want to drag down subtitles if the subtitle elevation is large
                -sStyle.elevation.toPx,
                // The lib uses Invisible instead of Gone for no reason
                binding.previewFrameLayout.height - binding.bottomPlayerBar.height
            ) else -sStyle.elevation.toPx
            ObjectAnimator.ofFloat(sView, "translationY", move.toFloat()).apply {
                duration = 200
                start()
            }
        }

    protected fun animateLayoutChanges() {
        if(isLayout(PHONE)) { // isEnabled also disables the onKeyDown
            playerBinding?.exoProgress?.isEnabled = isShowing // Prevent accidental clicks/drags
        }

        if (isShowing) {
            updateUIVisibility()
        } else {
            playerBinding?.playerHolder?.postDelayed({ updateUIVisibility() }, 200)
        }

        // Apply animation to the new top bar container
        val titleMove = if (isShowing) 0f else -50.toPx.toFloat()
        playerBinding?.playerTopBarContainer?.let {
            ObjectAnimator.ofFloat(it, "translationY", titleMove).apply {
                duration = 200
                start()
            }
        }

        val playerBarMove = if (isShowing) 0f else 50.toPx.toFloat()
        playerBinding?.bottomPlayerBar?.let {
            ObjectAnimator.ofFloat(it, "translationY", playerBarMove).apply {
                duration = 200
                start()
            }
        }

        val fadeTo = if (isShowing) 1f else 0f
        val fadeAnimation = AlphaAnimation(1f - fadeTo, fadeTo)

        fadeAnimation.duration = 100
        fadeAnimation.fillAfter = true

        animateLayoutChangesForSubtitles()

        val playerSourceMove = if (isShowing) 0f else -50.toPx.toFloat()

        playerBinding?.apply {
            playerOpenSource.let {
                ObjectAnimator.ofFloat(it, "translationY", playerSourceMove).apply {
                    duration = 200
                    start()
                }
            }

            if (!isLocked) {
                playerFfwdHolder.alpha = 1f
                playerRewHolder.alpha = 1f
                shadowOverlay.isVisible = true
                shadowOverlay.startAnimation(fadeAnimation)
                playerFfwdHolder.startAnimation(fadeAnimation)
                playerRewHolder.startAnimation(fadeAnimation)
                playerPausePlay.startAnimation(fadeAnimation)
                downloadBothHeader.startAnimation(fadeAnimation)
            }

            bottomPlayerBar.startAnimation(fadeAnimation)
            playerOpenSource.startAnimation(fadeAnimation)
            playerTopBarContainer.startAnimation(fadeAnimation) // Use the new top bar container
        }
    }

    @OptIn(UnstableApi::class)
    override fun subtitlesChanged() {
        val tracks = player.getVideoTracks()
        val isBuiltinSubtitles = tracks.currentTextTracks.all { track ->
            track.mimeType == MimeTypes.APPLICATION_MEDIA3_CUES
        }
        // Subtitle offset is not possible on built-in media3 tracks
        playerBinding?.playerSubtitleOffsetBtt?.isGone =
            isBuiltinSubtitles || tracks.currentTextTracks.isEmpty()
    }

    private fun restoreOrientationWithSensor(activity: Activity) {
        val currentOrientation = activity.resources.configuration.orientation
        val orientation = when (currentOrientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            Configuration.ORIENTATION_PORTRAIT ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

            else -> dynamicOrientation()
        }
        activity.requestedOrientation = orientation
    }

    private fun toggleOrientationWithSensor(activity: Activity) {
        val currentOrientation = activity.resources.configuration.orientation
        val orientation: Int = when (currentOrientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

            Configuration.ORIENTATION_PORTRAIT ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            else -> dynamicOrientation()
        }
        activity.requestedOrientation = orientation
    }

    open fun lockOrientation(activity: Activity) {
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        else activity.display!!
        val rotation = display.rotation
        val currentOrientation = activity.resources.configuration.orientation
        val orientation: Int
        when (currentOrientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                orientation =
                    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE

            Configuration.ORIENTATION_PORTRAIT ->
                orientation =
                    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

            else -> orientation = dynamicOrientation()
        }
        activity.requestedOrientation = orientation
    }

    private fun updateOrientation(ignoreDynamicOrientation: Boolean = false) {
        activity?.apply {
            if (lockRotation) {
                if (isLocked) {
                    lockOrientation(this)
                } else {
                    if (ignoreDynamicOrientation) {
                        // restore when lock is disabled
                        restoreOrientationWithSensor(this)
                    } else {
                        this.requestedOrientation = dynamicOrientation()
                    }
                }
            }
        }
    }

    protected fun enterFullscreen() {
        if (isFullScreenPlayer) {
            activity?.hideSystemUI()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && fullscreenNotch) {
                val params = activity?.window?.attributes
                params?.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                activity?.window?.attributes = params
            }
        }
        updateOrientation()
    }

    protected fun exitFullscreen() {
        //if (lockRotation)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        // simply resets brightness and notch settings that might have been overridden
        val lp = activity?.window?.attributes
        lp?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp?.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        activity?.window?.attributes = lp
        activity?.showSystemUI()
    }

    override fun onResume() {
        enterFullscreen()
        verifyVolume() // Assumed to be defined in AbstractPlayerFragment or a utility
        super.onResume()
    }

    override fun onDestroy() {
        exitFullscreen()
        player.release()
        player.releaseCallbacks()
        player = CS3IPlayer() // CS3IPlayer is assumed to be an existing class
        super.onDestroy()
    }

    private fun setPlayBackSpeed(speed: Float) {
        try {
            DataStoreHelper.playBackSpeed = speed
            playerBinding?.playerSpeedBtt?.text =
                getString(R.string.player_speed_text_format).format(speed)
                    .replace(".0x", "x")
        } catch (e: Exception) {
            // the format string was wrong
            logError(e)
        }

        player.setPlaybackSpeed(speed)
    }

    private fun skipOp() {
        player.seekTime(85000) // skip 85s
    }

    private fun showSubtitleOffsetDialog() {
        val ctx = context ?: return
        // Pause player because the subtitles cannot be continuously updated to follow playback.
        player.handleEvent(
            CSPlayerEvent.Pause,
            PlayerEventSource.UI
        )

        val binding = SubtitleOffsetBinding.inflate(LayoutInflater.from(ctx), null, false)

        // Use dialog as opposed to alertdialog to get fullscreen
        val dialog = Dialog(ctx, R.style.AlertDialogCustomBlack).apply {
            setContentView(binding.root)
        }
        dialog.show()

        val beforeOffset = subtitleDelay

        binding.apply {
            var subtitleAdapter: SubtitleOffsetItemAdapter? = null // SubtitleOffsetItemAdapter is assumed to be an existing class

            subtitleOffsetInput.doOnTextChanged { text, _, _, _ ->
                text?.toString()?.toLongOrNull()?.let { time ->
                    subtitleDelay = time

                    // Scroll to the first active subtitle
                    val playerPosition = player.getPosition() ?: 0
                    val totalPosition = playerPosition - subtitleDelay
                    subtitleAdapter?.updateTime(totalPosition)

                    subtitleAdapter?.getLatestActiveItem(totalPosition)
                        ?.let { subtitlePos ->
                            subtitleOffsetRecyclerview.scrollToPosition(subtitlePos)
                        }

                    val str = when {
                        time > 0L -> {
                            txt(R.string.subtitle_offset_extra_hint_later_format, time)
                        }

                        time < 0L -> {
                            txt(R.string.subtitle_offset_extra_hint_before_format, -time)
                        }

                        else -> {
                            txt(R.string.subtitle_offset_extra_hint_none_format)
                        }
                    }
                    subtitleOffsetSubTitle.setText(str)
                }
            }
            subtitleOffsetInput.text =
                Editable.Factory.getInstance()?.newEditable(beforeOffset.toString())

            val subtitles = player.getSubtitleCues().toMutableList()

            subtitleOffsetRecyclerview.isVisible = subtitles.isNotEmpty()
            noSubtitlesLoadedNotice.isVisible = subtitles.isEmpty()

            val initialSubtitlePosition = (player.getPosition() ?: 0) - subtitleDelay
            subtitleAdapter =
                SubtitleOffsetItemAdapter(initialSubtitlePosition, subtitles) { subtitleCue ->
                    val playerPosition = player.getPosition() ?: 0
                    subtitleOffsetInput.text = Editable.Factory.getInstance()
                        ?.newEditable((playerPosition - subtitleCue.startTimeMs).toString())
                }

            subtitleOffsetRecyclerview.adapter = subtitleAdapter
            // Prevent flashing changes when changing items
            (subtitleOffsetRecyclerview.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
                false

            val firstSubtitle = subtitleAdapter.getLatestActiveItem(initialSubtitlePosition)
            subtitleOffsetRecyclerview.scrollToPosition(firstSubtitle)

            val buttonChange = 100L
            val buttonChangeMore = 1000L

            fun changeBy(by: Long) {
                val current = (subtitleOffsetInput.text?.toString()?.toLongOrNull() ?: 0) + by
                subtitleOffsetInput.text =
                    Editable.Factory.getInstance()?.newEditable(current.toString())
            }

            subtitleOffsetAdd.setOnClickListener {
                changeBy(buttonChange)
            }
            subtitleOffsetAddMore.setOnClickListener {
                changeBy(buttonChangeMore)
            }
            subtitleOffsetSubtract.setOnClickListener {
                changeBy(-buttonChange)
            }
            subtitleOffsetSubtractMore.setOnClickListener {
                changeBy(-buttonChangeMore)
            }

            dialog.setOnDismissListener {
                if (isFullScreenPlayer)
                    activity?.hideSystemUI()
            }
            applyBtt.setOnClickListener {
                dialog.dismissSafe(activity)
                player.seekTime(1L)
            }
            resetBtt.setOnClickListener {
                subtitleDelay = 0
                dialog.dismissSafe(activity)
                player.seekTime(1L)
            }
            cancelBtt.setOnClickListener {
                subtitleDelay = beforeOffset
                dialog.dismissSafe(activity)
            }
        }
    }


    private fun showSpeedDialog() {
        val speedsText =
            listOf(
                "0.5x",
                "0.75x",
                "0.85x",
                "1x",
                "1.15x",
                "1.25x",
                "1.4x",
                "1.5x",
                "1.75x",
                "2x"
            )
        val speedsNumbers =
            listOf(0.5f, 0.75f, 0.85f, 1f, 1.15f, 1.25f, 1.4f, 1.5f, 1.75f, 2f)
        val speedIndex = speedsNumbers.indexOf(player.getPlaybackSpeed())

        activity?.let { act ->
            act.showDialog(
                speedsText,
                speedIndex,
                act.getString(R.string.player_speed),
                false,
                {
                    if (isFullScreenPlayer)
                        activity?.hideSystemUI()
                }) { index ->
                if (isFullScreenPlayer)
                    activity?.hideSystemUI()
                setPlayBackSpeed(speedsNumbers[index])
            }
        }
    }

    fun resetRewindText() {
        playerBinding?.exoRewText?.text =
            getString(R.string.rew_text_regular_format).format(fastForwardTime / 1000)
    }

    fun resetFastForwardText() {
        playerBinding?.exoFfwdText?.text =
            getString(R.string.ffw_text_regular_format).format(fastForwardTime / 1000)
    }

    private fun rewind() {
        try {
            playerBinding?.apply {
                playerCenterMenu.isGone = false
                playerRewHolder.alpha = 1f

                val rotateLeft = AnimationUtils.loadAnimation(context, R.anim.rotate_left)
                playerRew.startAnimation(rotateLeft)

                val goLeft = AnimationUtils.loadAnimation(context, R.anim.go_left)
                goLeft.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationRepeat(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        exoRewText.post {
                            resetRewindText()
                            playerCenterMenu.isGone = !isShowing
                            playerRewHolder.alpha = if (isShowing) 1f else 0f
                        }
                    }
                })
                exoRewText.startAnimation(goLeft)
                exoRewText.text =
                    getString(R.string.rew_text_format).format(fastForwardTime / 1000)
            }
            player.seekTime(-fastForwardTime)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun fastForward() {
        try {
            playerBinding?.apply {
                playerCenterMenu.isGone = false
                playerFfwdHolder.alpha = 1f

                val rotateRight = AnimationUtils.loadAnimation(context, R.anim.rotate_right)
                playerFfwd.startAnimation(rotateRight)

                val goRight = AnimationUtils.loadAnimation(context, R.anim.go_right)
                goRight.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationRepeat(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        exoFfwdText.post {
                            resetFastForwardText()
                            playerCenterMenu.isGone = !isShowing
                            playerFfwdHolder.alpha = if (isShowing) 1f else 0f
                        }
                    }
                })
                exoFfwdText.startAnimation(goRight)
                exoFfwdText.text =
                    getString(R.string.ffw_text_format).format(fastForwardTime / 1000)
            }
            player.seekTime(fastForwardTime)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun onClickChange() {
        isShowing = !isShowing
        if (isShowing) {
            playerBinding?.playerIntroPlay?.isGone = true
            autoHide() // Assumed to be defined in AbstractPlayerFragment or a utility
        }
        if (isFullScreenPlayer)
            activity?.hideSystemUI()
        animateLayoutChanges()
        playerBinding?.playerPausePlay?.requestFocus()
    }

    private fun toggleLock() {
        if (!isShowing) {
            onClickChange()
        }

        isLocked = !isLocked
        updateOrientation(true) // set true to ignore auto rotate to stay in current orientation

        if (isLocked && isShowing) {
            playerBinding?.playerHolder?.postDelayed({
                if (isLocked && isShowing) {
                    onClickChange()
                }
            }, 200)
        }

        val fadeTo = if (isLocked) 0f else 1f
        playerBinding?.apply {
            val fadeAnimation = AlphaAnimation(playerVideoTitle.alpha, fadeTo).apply {
                duration = 100
                fillAfter = true
            }

            updateUIVisibility()
            // MENUS
            playerPausePlay.startAnimation(fadeAnimation)
            playerFfwdHolder.startAnimation(fadeAnimation)
            playerRewHolder.startAnimation(fadeAnimation)
            downloadBothHeader.startAnimation(fadeAnimation)

            // TITLE - Apply animation to the container
            playerTitleInfoContainer.startAnimation(fadeAnimation)
            playerEpisodeFiller.startAnimation(fadeAnimation) // This might need re-evaluation if its position changes
            playerTopBarContainer.startAnimation(fadeAnimation) // Use the new top bar container

            // BOTTOM
            // playerLockHolder is removed from XML, playerLock is now directly in playerTopBarContainer
            // player_go_back_holder?.startAnimation(fadeAnimation) is now part of playerTopBarContainer animation

            shadowOverlay.isVisible = true
            shadowOverlay.startAnimation(fadeAnimation)
        }
        updateLockUI()
    }

    open fun updateUIVisibility() {
        val isGone = isLocked || !isShowing
        var togglePlayerTitleGone = isGone
        context?.let {
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(it)
            val limitTitle = settingsManager.getInt(getString(R.string.prefer_limit_title_key), 0)
            if (limitTitle < 0) {
                togglePlayerTitleGone = true
            }
        }
        playerBinding?.apply {
            // playerLockHolder is removed, playerLock is directly in playerTopBarContainer
            // playerLock.isGone = !isShowing // This is handled by the parent container's visibility
            playerVideoBar.isGone = isGone // Bottom progress bar

            playerPausePlay.isGone = isGone
            playerTopBarContainer.isGone = isGone // Hide/show the entire top bar
            playerEpisodeFiller.isGone = isGone
            playerCenterMenu.isGone = isGone
            playerGoBackHolder.isGone = isGone // This is now part of playerTopBarContainer, so its visibility is controlled by its parent
            playerSourcesBtt.isGone = isGone
            playerSubtitleOffsetBtt.isGone = isGone // Ensure new buttons also hide
            playerSpeedBtt.isGone = isGone // Ensure new buttons also hide
            playerPrevChannelBtt.isGone = isGone // Ensure new buttons also hide
            playerNextChannelBtt.isGone = isGone // Ensure new buttons also hide

            // Title visibility is now controlled by its container
            playerTitleInfoContainer.isGone = togglePlayerTitleGone
            // player_video_title_rez?.isGone = isGone // Handled by playerTitleInfoContainer

            playerLock.isGone = !isShowing // Still control the lock button specifically if needed
            playerSkipEpisode.isClickable = !isGone // Assuming playerSkipEpisode exists elsewhere in XML
        }
    }

    private fun updateLockUI() {
        playerBinding?.apply {
            playerLock.setIconResource(if (isLocked) R.drawable.video_locked else R.drawable.video_unlocked)
            // Apply color tinting to MaterialButton icon and ripple
            val color = if (isLocked) context?.colorFromAttribute(R.attr.colorPrimary)
            else Color.WHITE
            if (color != null) {
                playerLock.iconTint = ColorStateList.valueOf(color)
                playerLock.rippleColor =
                    ColorStateList.valueOf(Color.argb(50, color.red, color.green, color.blue))
            }
        }
    }

    private var currentTapIndex = 0

    // Placeholder for dynamicOrientation - replace with actual logic if available
    private fun dynamicOrientation(): Int {
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED // Or SENSOR, USER, etc. based on desired default
    }

    // Placeholder for autoHide - replace with actual logic if available
    protected fun autoHide() {
        // Implement logic to hide controls after a delay
        // Example: Handler().postDelayed({ if (isShowing && !isLocked) onClickChange() }, 3000L)
    }

    // Placeholder for verifyVolume - assumed to be in AbstractPlayerFragment or a utility
    protected fun verifyVolume() {
        // Implement volume verification logic
    }

    // Start of GeneratorPlayer specific implementation
    @UnstableApi
    class GeneratorPlayer : FullScreenPlayer() {
        companion object {
            const val NOTIFICATION_ID = 2326
            const val CHANNEL_ID = 7340
            const val STOP_ACTION = "stopcs3"

            private var lastUsedGenerator: IGenerator? = null
            fun newInstance(generator: IGenerator, syncData: HashMap<String, String>? = null): Bundle {
                Log.i(TAG, "newInstance = $syncData")
                lastUsedGenerator = generator
                return Bundle().apply {
                    if (syncData != null) putSerializable("syncData", syncData)
                }
            }

            val subsProviders
                get() = subtitleProviders.filter { provider ->
                    (provider as? AbstractSubApi)?.let { !it.requiresLogin || it.loginInfo() != null }
                        ?: true
                }.map { SubRepository(it) }
            val subsProvidersIsActive
                get() = subsProviders.isNotEmpty()
        }


        private var titleRez = 3
        private var limitTitle = 0

        private lateinit var viewModel: PlayerGeneratorViewModel //by activityViewModels()
        private lateinit var sync: SyncViewModel
        private var currentLinks: Set<Pair<ExtractorLink?, ExtractorUri?>> = setOf()
        private var currentSubs: Set<SubtitleData> = setOf()

        private var currentSelectedLink: Pair<ExtractorLink?, ExtractorUri?>? = null
        private var currentSelectedSubtitles: SubtitleData? = null
        private var currentMeta: Any? = null
        private var nextMeta: Any? = null
        private var isActive: Boolean = false
        private var isNextEpisode: Boolean = false // this is used to reset the watch time

        private var preferredAutoSelectSubtitles: String? = null // null means do nothing, "" means none

        private var binding: FragmentPlayerBinding? = null // This binding is for FragmentPlayerBinding, not PlayerCustomLayoutBinding

        private fun startLoading() {
            player.release()
            currentSelectedSubtitles = null
            isActive = false
            binding?.overlayLoadingSkipButton?.isVisible = false
            binding?.playerLoadingOverlay?.isVisible = true
        }

        private fun setSubtitles(subtitle: SubtitleData?): Boolean {
            // If subtitle is changed -> Save the language
            if (subtitle != currentSelectedSubtitles) {
                val subtitleLanguage639 = if (subtitle == null) {
                    // "" is No Subtitles
                    ""
                } else if (subtitle.languageCode != null) {
                    // Could be "English 4" which is why it is trimmed.
                    val trimmedLanguage = subtitle.languageCode.replace(Regex("\\d"), "").trim()

                    languages.firstOrNull { language ->
                        language.languageName.equals(trimmedLanguage, ignoreCase = true) ||
                                language.ISO_639_1 == subtitle.languageCode
                    }?.ISO_639_1
                } else {
                    null
                }

                if (subtitleLanguage639 != null) {
                    setKey(SUBTITLE_AUTO_SELECT_KEY, subtitleLanguage639)
                    preferredAutoSelectSubtitles = subtitleLanguage639
                }
            }

            currentSelectedSubtitles = subtitle
            //Log.i(TAG, "setSubtitles = $subtitle")
            return player.setPreferredSubtitles(subtitle)
        }

        override fun embeddedSubtitlesFetched(subtitles: List<SubtitleData>) {
            viewModel.addSubtitles(subtitles.toSet())
        }

        override fun onTracksInfoChanged() {
            val tracks = player.getVideoTracks()
            playerBinding?.playerTracksBtt?.isVisible =
                tracks.allVideoTracks.size > 1 || tracks.allAudioTracks.size > 1
            // Only set the preferred language if it is available.
            // Otherwise it may give some users audio track init failed!
            if (tracks.allAudioTracks.any { it.language == preferredAudioTrackLanguage }) {
                player.setPreferredAudioTrack(preferredAudioTrackLanguage)
            }
        }

        override fun playerStatusChanged() {
            super.playerStatusChanged()
            if (player.getIsPlaying()) {
                viewModel.forceClearCache = false
            }
        }

        private fun noSubtitles(): Boolean {
            return setSubtitles(null)
        }

        private fun getPos(): Long {
            val durPos = DataStoreHelper.getViewPos(viewModel.getId()) ?: return 0L
            if (durPos.duration == 0L) return 0L
            if (durPos.position * 100L / durPos.position > 95L) { // Corrected from durPos.duration to durPos.position
                return 0L
            }
            return durPos.position
        }

        private var currentVerifyLink: Job? = null

        private fun loadExtractorJob(extractorLink: ExtractorLink?) {
            currentVerifyLink?.cancel()

            extractorLink?.let { link ->
                currentVerifyLink = ioSafe {
                    if (link.extractorData != null) {
                        getApiFromNameNull(link.source)?.extractorVerifierJob(link.extractorData)
                    }
                }
            }
        }

        // https://github.com/androidx/media/blob/main/libraries/ui/src/main/java/androidx/media3/ui/PlayerNotificationManager.java#L1517
        private fun createBroadcastIntent(
            action: String,
            context: Context,
            instanceId: Int
        ): PendingIntent {
            val intent: Intent = Intent(action).setPackage(context.packageName)
            intent.putExtra(EXTRA_INSTANCE_ID, instanceId)
            val pendingFlags = if (Util.SDK_INT >= 23) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            return PendingIntent.getBroadcast(context, instanceId, intent, pendingFlags)
        }

        @OptIn(UnstableApi::class)
        @UnstableApi
        private var cachedPlayerNotificationManager: PlayerNotificationManager? = null

        @OptIn(UnstableApi::class)
        @UnstableApi
        private fun getMediaNotification(context: Context): PlayerNotificationManager {
            val cache = cachedPlayerNotificationManager
            if (cache != null) return cache
            return PlayerNotificationManager.Builder(
                context,
                NOTIFICATION_ID,
                CHANNEL_ID.toString()
            )
                .setChannelNameResourceId(R.string.player_notification_channel_name)
                .setChannelDescriptionResourceId(R.string.player_notification_channel_description)
                .setMediaDescriptionAdapter(object : MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        return when (val meta = currentMeta) {
                            is ResultEpisode -> {
                                meta.headerName
                            }

                            is ExtractorUri -> {
                                meta.headerName ?: meta.name
                            }

                            else -> null
                        } ?: "Unknown"
                    }

                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        // Open the app without creating a new task to resume playback seamlessly
                        return PendingIntentCompat.getActivity(
                            context,
                            0,
                            Intent(context, MainActivity::class.java),
                            0,
                            false
                        )
                    }

                    override fun getCurrentContentText(player: Player): CharSequence? {
                        return when (val meta = currentMeta) {
                            is ResultEpisode -> {
                                meta.name
                            }

                            is ExtractorUri -> {
                                if (meta.headerName == null) {
                                    null
                                } else {
                                    meta.name
                                }
                            }

                            else -> null
                        }
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ): Bitmap? {
                        ioSafe {
                            val url = when (val meta = currentMeta) {
                                is ResultEpisode -> {
                                    meta.poster
                                }

                                else -> null
                            }
                            // if we have a poster url try with it first
                            if (url != null) {
                                val urlBitmap = context.getImageBitmapFromUrl(url)
                                if (urlBitmap != null) {
                                    callback.onBitmap(urlBitmap)
                                    return@ioSafe
                                }
                            }

                            // retry several times with a preview in case the preview generator is slow
                            for (i in 0..10) {
                                val preview = this@GeneratorPlayer.player.getPreview(0.5f)
                                if (preview == null) {
                                    delay(1000L)
                                    continue
                                }
                                callback.onBitmap(
                                    preview
                                )
                                break
                            }
                        }

                        // return null as we want to use the callback
                        return null
                    }
                }).setCustomActionReceiver(object : PlayerNotificationManager.CustomActionReceiver {
                    // we have to use a custom action for stop if we want to exit the player instead of just stopping playback
                    override fun createCustomActions(
                        context: Context,
                        instanceId: Int
                    ): MutableMap<String, NotificationCompat.Action> {
                        return mutableMapOf(
                            STOP_ACTION to NotificationCompat.Action(
                                R.drawable.baseline_stop_24,
                                context.getString(androidx.media3.ui.R.string.exo_controls_stop_description),
                                createBroadcastIntent(STOP_ACTION, context, instanceId)
                            )
                        )
                    }

                    override fun getCustomActions(player: Player): MutableList<String> {
                        return mutableListOf(STOP_ACTION)
                    }

                    override fun onCustomAction(player: Player, action: String, intent: Intent) {
                        when (action) {
                            STOP_ACTION -> {
                                exitFullscreen()
                                this@GeneratorPlayer.player.release()
                                activity?.popCurrentPage()
                            }
                        }
                    }
                })
                .setPlayActionIconResourceId(R.drawable.ic_baseline_play_arrow_24)
                .setPauseActionIconResourceId(R.drawable.netflix_pause)
                .setSmallIconResourceId(R.drawable.baseline_headphones_24)
                .setStopActionIconResourceId(R.drawable.baseline_stop_24)
                .setRewindActionIconResourceId(R.drawable.go_back_30)
                .setFastForwardActionIconResourceId(R.drawable.go_forward_30)
                .setNextActionIconResourceId(R.drawable.ic_baseline_skip_next_24)
                .setPreviousActionIconResourceId(R.drawable.baseline_skip_previous_24)
                .build().apply {
                    setColorized(true) // Color
                    setUseChronometer(true) // Seekbar

                    // Don't show the prev episode button
                    setUsePreviousAction(false)
                    setUsePreviousActionInCompactView(false)

                    // Don't show the next episode button
                    setUseNextAction(false)
                    setUseNextActionInCompactView(false)

                    // Show the skip 30s in both modes
                    setUseFastForwardAction(true)
                    setUseFastForwardActionInCompactView(true)

                    // Only show rewind in expanded
                    setUseRewindAction(true)
                    setUseRewindActionInCompactView(false) // Corrected from setUseFastForwardActionInCompactView(false)

                    // Use custom stop action
                    setUseStopAction(false)
                }
                .also { cachedPlayerNotificationManager = it }
        }

        override fun playerUpdated(player: Any?) {
            super.playerUpdated(player)

            // Cancel the notification when released
            if (player == null) {
                cachedPlayerNotificationManager?.setPlayer(null)
                cachedPlayerNotificationManager = null
                return
            }

            // setup the notification when starting the player
            if (player is ExoPlayer) {
                val ctx = context ?: return
                getMediaNotification(ctx).apply {
                    setPlayer(player)
                    mMediaSession?.platformToken?.let { // mMediaSession is assumed to be defined by PlayerNotificationManager
                        setMediaSessionToken(it)
                    }
                }
            }
        }

        override fun onDownload(event: DownloadEvent) {
            super.onDownload(event)
            showDownloadProgress(event)
        }

        private fun showDownloadProgress(event: DownloadEvent) {
            activity?.runOnUiThread {
                playerBinding?.downloadedProgress?.apply {
                    val indeterminate = event.totalBytes <= 0 || event.downloadedBytes <= 0
                    isIndeterminate = indeterminate
                    if (!indeterminate) {
                        max = (event.totalBytes / 1000).toInt()
                        progress = (event.downloadedBytes / 1000).toInt()
                    }
                }
                playerBinding?.downloadedProgressText.setText(
                    txt(
                        R.string.download_size_format,
                        android.text.format.Formatter.formatShortFileSize(
                            context,
                            event.downloadedBytes
                        ),
                        android.text.format.Formatter.formatShortFileSize(context, event.totalBytes)
                    )
                )
                val downloadSpeed =
                    android.text.format.Formatter.formatShortFileSize(context, event.downloadSpeed)
                playerBinding?.downloadedProgressSpeedText?.text =
                // todo string fmt
                    event.connections?.let { connections ->
                        "%s/s - %d Connections".format(downloadSpeed, connections)
                    } ?: downloadSpeed

                // don't display when done
                playerBinding?.downloadedProgressSpeedText?.isGone =
                    event.downloadedBytes != 0L && event.downloadedBytes - 1024 >= event.totalBytes
            }
        }

        private fun loadLink(link: Pair<ExtractorLink?, ExtractorUri?>?, sameEpisode: Boolean) {
            if (link == null) return

            // manage UI
            binding?.playerLoadingOverlay?.isVisible = false
            val isTorrent =
                link.first?.type == ExtractorLinkType.MAGNET || link.first?.type == ExtractorLinkType.TORRENT

            playerBinding?.downloadHeader?.isVisible = false
            playerBinding?.downloadHeaderToggle?.isVisible = isTorrent

            showDownloadProgress(DownloadEvent(0, 0, 0, null))

            uiReset() // Assumed to be defined in AbstractPlayerFragment or a utility
            currentSelectedLink = link
            currentMeta = viewModel.getMeta()
            nextMeta = viewModel.getNextMeta()
            //  setEpisodes(viewModel.getAllMeta() ?: emptyList())
            isActive = true
            setPlayerDimen(null) // Assumed to be defined in AbstractPlayerFragment or a utility
            setTitle() // Assumed to be defined in AbstractPlayerFragment or a utility
            if (!sameEpisode)
                hasRequestedStamps = false // Assumed to be defined in AbstractPlayerFragment or a utility

            loadExtractorJob(link.first)
            // load player
            context?.let { ctx ->
                val (url, uri) = link
                player.loadPlayer(
                    ctx,
                    sameEpisode,
                    url,
                    uri,
                    startPosition = if (sameEpisode) null else {
                        if (isNextEpisode) 0L else getPos()
                    },
                    currentSubs,
                    (if (sameEpisode) currentSelectedSubtitles else null) ?: getAutoSelectSubtitle( // getAutoSelectSubtitle is assumed to be defined
                        currentSubs, settings = true, downloads = true
                    ),
                    preview = isFullScreenPlayer
                )
            }

            if (!sameEpisode)
                player.addTimeStamps(listOf()) // clear stamps
        }

        private fun closestQuality(target: Int?): Qualities {
            if (target == null) return Qualities.Unknown
            return Qualities.entries.minBy { abs(it.value - target) }
        }

        private fun getLinkPriority(
            qualityProfile: Int,
            link: Pair<ExtractorLink?, ExtractorUri?>
        ): Int {
            val (linkData, _) = link

            val qualityPriority = QualityDataHelper.getQualityPriority(
                qualityProfile,
                closestQuality(linkData?.quality)
            )
            val sourcePriority =
                QualityDataHelper.getSourcePriority(qualityProfile, linkData?.source)

            // negative because we want to sort highest quality first
            return qualityPriority + sourcePriority
        }

        private fun sortLinks(qualityProfile: Int): List<Pair<ExtractorLink?, ExtractorUri?>> {
            return currentLinks.sortedBy {
                -getLinkPriority(qualityProfile, it)
            }
        }

        data class TempMetaData(
            var episode: Int? = null,
            var season: Int? = null,
            var name: String? = null,
            var imdbId: String? = null,
        )

        private fun getMetaData(): TempMetaData {
            val meta = TempMetaData()

            when (val newMeta = currentMeta) {
                is ResultEpisode -> {
                    if (!newMeta.tvType.isMovieType()) {
                        meta.episode = newMeta.episode
                        meta.season = newMeta.season
                    }
                    meta.name = newMeta.headerName
                }

                is ExtractorUri -> {
                    if (newMeta.tvType?.isMovieType() == false) {
                        meta.episode = newMeta.episode
                        meta.season = newMeta.season
                    }
                    meta.name = newMeta.headerName
                }
            }
            return meta
        }

        fun getName(entry: AbstractSubtitleEntities.SubtitleEntity, withLanguage: Boolean): String {
            if (entry.lang.isBlank() || !withLanguage) {
                return entry.name
            }
            val language = fromTwoLettersToLanguage(entry.lang.trim()) ?: entry.lang
            return "$language ${entry.name}"
        }

        override fun openOnlineSubPicker(
            context: Context, loadResponse: LoadResponse?, dismissCallback: (() -> Unit)
        ) {
            val providers = subsProviders
            val isSingleProvider = subsProviders.size == 1

            val dialog = Dialog(context, R.style.AlertDialogCustomBlack)
            val binding =
                DialogOnlineSubtitlesBinding.inflate(LayoutInflater.from(context), null, false)
            dialog.setContentView(binding.root)

            var currentSubtitles: List<AbstractSubtitleEntities.SubtitleEntity> = emptyList()
            var currentSubtitle: AbstractSubtitleEntities.SubtitleEntity? = null


            val layout = R.layout.sort_bottom_single_choice_double_text
            val arrayAdapter =
                object : ArrayAdapter<AbstractSubtitleEntities.SubtitleEntity>(dialog.context, layout) {
                    fun setHearingImpairedIcon(
                        imageViewEnd: ImageView?, position: Int
                    ) {
                        if (imageViewEnd == null) return
                        val isHearingImpaired =
                            currentSubtitles.getOrNull(position)?.isHearingImpaired ?: false

                        val drawableEnd = if (isHearingImpaired) {
                            ContextCompat.getDrawable(
                                context, R.drawable.ic_baseline_hearing_24
                            )?.apply {
                                setTint(
                                    ContextCompat.getColor(
                                        context, R.color.textColor
                                    )
                                )
                            }
                        } else null

                        imageViewEnd.setImageDrawable(drawableEnd)
                    }

                    @SuppressLint("SetTextI18n")
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: LayoutInflater.from(context).inflate(layout, null)

                        val item = getItem(position)

                        val mainTextView = view.findViewById<TextView>(R.id.main_text)
                        val secondaryTextView = view.findViewById<TextView>(R.id.secondary_text)
                        val drawableEnd = view.findViewById<ImageView>(R.id.drawable_end)

                        mainTextView?.text = item?.let { getName(it, false) }

                        val language =
                            item?.let { fromTwoLettersToLanguage(it.lang.trim()) ?: it.lang } ?: ""
                        val providerSuffix =
                            if (isSingleProvider || item == null) "" else " · ${item.source}"
                        secondaryTextView?.text = language + providerSuffix

                        setHearingImpairedIcon(drawableEnd, position)
                        return view
                    }
                }

            dialog.show()
            binding.cancelBtt.setOnClickListener {
                dialog.dismissSafe()
            }

            binding.subtitleAdapter.choiceMode = AbsListView.CHOICE_MODE_SINGLE
            binding.subtitleAdapter.adapter = arrayAdapter

            binding.subtitleAdapter.setOnItemClickListener { _, _, position, _ ->
                currentSubtitle = currentSubtitles.getOrNull(position) ?: return@setOnItemClickListener
            }

            var currentLanguageTwoLetters: String = getAutoSelectLanguageISO639_1()


            fun setSubtitlesList(list: List<AbstractSubtitleEntities.SubtitleEntity>) {
                currentSubtitles = list
                arrayAdapter.clear()
                arrayAdapter.addAll(currentSubtitles)
            }

            val currentTempMeta = getMetaData()

            // bruh idk why it is not correct
            val color =
                ColorStateList.valueOf(context.colorFromAttribute(androidx.appcompat.R.attr.colorAccent))
            binding.searchLoadingBar.progressTintList = color
            binding.searchLoadingBar.indeterminateTintList = color

            observeNullable(viewModel.currentSubtitleYear) {
                // When year is changed search again
                binding.subtitlesSearch.setQuery(binding.subtitlesSearch.query, true)
                binding.yearBtt.text = it?.toString() ?: txt(R.string.none).asString(context)
            }

            binding.yearBtt.setOnClickListener {
                val none = txt(R.string.none).asString(context)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val earliestYear = 1900

                val years = (currentYear downTo earliestYear).toList()
                val options = listOf(none) + years.map {
                    it.toString()
                }

                val selectedIndex = viewModel.currentSubtitleYear.value
                    ?.let {
                        // + 1 since none also takes a space
                        years.indexOf(it) + 1
                    }
                    ?.takeIf { it >= 0 } ?: 0

                activity?.showDialog(
                    options,
                    selectedIndex,
                    txt(R.string.year).asString(context),
                    true, {
                    }, { index ->
                        viewModel.setSubtitleYear(years.getOrNull(index - 1))
                    }
                )
            }

            binding.subtitlesSearch.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    binding.searchLoadingBar.show()
                    ioSafe {
                        val search =
                            SubtitleSearch(
                                query = query ?: return@ioSafe,
                                imdbId = loadResponse?.getImdbId(),
                                tmdbId = loadResponse?.getTMDbId()?.toInt(),
                                malId = loadResponse?.getMalId()?.toInt(),
                                aniListId = loadResponse?.getAniListId()?.toInt(),
                                epNumber = currentTempMeta.episode,
                                seasonNumber = currentTempMeta.season,
                                lang = currentLanguageTwoLetters.ifBlank { null },
                                year = viewModel.currentSubtitleYear.value
                            )

                        // TODO Make ui a lot better, like search with tabs
                        val results = providers.amap {
                            when (val response = it.search(search)) {
                                is Resource.Success -> {
                                    response.value
                                }

                                is Resource.Loading -> {
                                    emptyList()
                                }

                                is Resource.Failure -> {
                                    showToast(response.errorString)
                                    emptyList()
                                }
                            }
                        }
                        val max = results.maxOfOrNull { it.size } ?: return@ioSafe

                        // very ugly
                        val items = ArrayList<AbstractSubtitleEntities.SubtitleEntity>()
                        val arrays = results.size
                        for (index in 0 until max) {
                            for (i in 0 until arrays) {
                                items.add(results[i].getOrNull(index) ?: continue)
                            }
                        }

                        // ugly ik
                        activity?.runOnUiThread {
                            setSubtitlesList(items.sortSubs(currentLanguageTwoLetters)) // Sort subtitles by language
                            binding.searchLoadingBar.hide()
                            if (items.isEmpty()) {
                                showToast(R.string.no_results_found)
                            }
                        }
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return true
                }
            })

            binding.languageBtt.setOnClickListener {
                activity?.showDialog(
                    languages.map { it.languageName },
                    languages.indexOfFirst { it.ISO_639_1 == currentLanguageTwoLetters },
                    txt(R.string.languages).asString(context),
                    true, {
                        // On dismiss
                    }, { index ->
                        currentLanguageTwoLetters = languages[index].ISO_639_1
                        binding.languageBtt.text = languages[index].languageName
                        // Trigger search with new language
                        binding.subtitlesSearch.setQuery(binding.subtitlesSearch.query, true)
                    }
                )
            }

            binding.applyBtt.setOnClickListener {
                val selectedSubtitle = currentSubtitle
                if (selectedSubtitle != null) {
                    viewModel.viewModelScope.launch {
                        val subtitleData = SubtitleData(
                            selectedSubtitle.url,
                            selectedSubtitle.name,
                            selectedSubtitle.lang,
                            selectedSubtitle.isVtt
                        )
                        setSubtitles(subtitleData)
                        dialog.dismissSafe()
                        dismissCallback()
                    }
                } else {
                    showToast(R.string.no_subtitle_selected)
                }
            }

            binding.noSubtitlesBtt.setOnClickListener {
                noSubtitles()
                dialog.dismissSafe()
                dismissCallback()
            }

            // Initial search when dialog opens
            binding.subtitlesSearch.setQuery(currentTempMeta.name, true)
            binding.languageBtt.text = fromTwoLettersToLanguage(currentLanguageTwoLetters) ?: currentLanguageTwoLetters

            dialog.setOnDismissListener {
                // Resume player when dialog is dismissed
                player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)
            }
        }
    }
}
