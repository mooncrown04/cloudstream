package com.lagradost.cloudstream3.ui.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
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
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.button.MaterialButton
import com.lagradost.cloudstream3.CommonActivity.keyEventListener
import com.lagradost.cloudstream3.CommonActivity.playerEventListener
import com.lagradost.cloudstream3.CommonActivity.screenHeight
import com.lagradost.cloudstream3.CommonActivity.screenWidth
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.PlayerCustomLayoutBinding
import com.lagradost.cloudstream3.databinding.SubtitleOffsetBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.player.GeneratorPlayer.Companion.subsProvidersIsActive
import com.lagradost.cloudstream3.ui.player.source_priority.QualityDataHelper
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.AppContextUtils.isUsingMobileData
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showDialog
import com.lagradost.cloudstream3.utils.UIHelper.colorFromAttribute
import com.lagradost.cloudstream3.utils.UIHelper.dismissSafe
import com.lagradost.cloudstream3.utils.UIHelper.getNavigationBarHeight
import com.lagradost.cloudstream3.utils.UIHelper.getStatusBarHeight
import com.lagradost.cloudstream3.utils.UIHelper.hideSystemUI
import com.lagradost.cloudstream3.utils.UIHelper.popCurrentPage
import com.lagradost.cloudstream3.utils.UIHelper.showSystemUI
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream3.utils.UserPreferenceDelegate
import com.lagradost.cloudstream3.utils.Vector2
import com.lagradost.cloudstream3.utils.setText
import com.lagradost.cloudstream3.utils.txt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt


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
}
