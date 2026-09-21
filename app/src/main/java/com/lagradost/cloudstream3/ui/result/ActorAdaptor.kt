package com.lagradost.cloudstream3.ui.result

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import androidx.core.view.isVisible
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.ActorRole
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.CastItemBinding
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.NoStateAdapter
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.newSharedPool
import com.lagradost.cloudstream3.ui.quicksearch.QuickSearchFragment
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage

class ActorAdaptor(
    private var nextFocusUpId: Int? = null,
    private val focusCallback: (View?) -> Unit = {}
) : NoStateAdapter<ActorData>(diffCallback = BaseDiffCallback(itemSame = { a, b ->
    a.actor.name == b.actor.name
})) {
    companion object {
        val sharedPool =
            newSharedPool { setMaxRecycledViews(CONTENT, 10) }
    }

    // Seslendirme / Oyuncu resmi değişim durumunu saklar
    val inverted: HashMap<ActorData, Boolean> = hashMapOf()

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        return ViewHolderState(
            CastItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onClearView(holder: ViewHolderState<Any>) {
        when (val binding = holder.view) {
            is CastItemBinding -> {
                clearImage(binding.actorImage)
            }
        }
    }

    override fun onUpdateContent(holder: ViewHolderState<Any>, item: ActorData, position: Int) {
        when (val binding = holder.view) {
            is CastItemBinding -> {
                val anim: Animation = ScaleAnimation(
                    0.8f, 1f,
                    0.8f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
                anim.fillAfter = true
                anim.duration = 200
                anim.interpolator = OvershootInterpolator()
                binding.voiceActorImageHolder2.startAnimation(anim)
            }
        }
        super.onUpdateContent(holder, item, position)
    }

    override fun onBindContent(holder: ViewHolderState<Any>, item: ActorData, position: Int) {
        when (val binding = holder.view) {
            is CastItemBinding -> {
                val itemView = binding.root
                val isInverted = inverted.getOrDefault(item, false)

                val (mainImg, vaImage) = if (!isInverted || item.voiceActor?.image.isNullOrBlank()) {
                    Pair(item.actor.image, item.voiceActor?.image)
                } else {
                    Pair(item.voiceActor?.image, item.actor.image)
                }

                // Android TV / Odaklanma (Focus) sırasının RecyclerView dışına kaçmasını engeller
                if (position == 0) {
                    itemView.nextFocusLeftId = R.id.result_cast_items
                } else if ((position - 1) == itemCount) {
                    itemView.nextFocusRightId = R.id.result_cast_items
                }
                nextFocusUpId?.let {
                    itemView.nextFocusUpId = it
                }

                itemView.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        focusCallback(v)
                    }
                }

                // =========================================================================
                // 1. PENCERE / EYLEM: NORMAL KISA TIKLAMA (OK Tuşu / Ekrana Dokunma)
                // Oyuncunun tüm film/dizilerini ve biyografisini gösteren alt sayfayı (ActorFilmography) açar.
                // =========================================================================
                itemView.setOnClickListener {
                    ActorFilmography.show(itemView.context, item.voiceActor ?: item.actor)
                }

                // =========================================================================
                // 2. PENCERE / EYLEM: UZUN BASMA (OK Tuşuna Basılı Tutma)
                // Oyuncunun sadece kısa biyografisini gösteren pop-up penceresini (ActorInfoDialog) açar.
                // =========================================================================
                itemView.setOnLongClickListener {
                    ActorInfoDialog.show(itemView.context, item.voiceActor ?: item.actor)
                    true
                }

                // =========================================================================
                // 3. PENCERE / EYLEM: TV KUMANDASI ÖZEL TUŞ BASIMI
                // Kumandadaki Oynat/Durdur, Bilgi (Info) veya Renkli tuşlara basıldığında
                // oyuncunun ismiyle hızlı tam ekran aramayı (QuickSearchFragment) başlatır.
                // =========================================================================
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_PROG_YELLOW,
                            KeyEvent.KEYCODE_INFO -> {
                                // Oyuncunun adı ile genel aramayı tetikler
                                val targetActor = item.voiceActor ?: item.actor
                                QuickSearchFragment.pushSearch(
                                    autoSearch = targetActor.name
                                )
                                true // Tuş eyleminin başarıyla işlendiğini bildirir
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }

                // Arayüz bileşenlerine veri ve görsellerin bağlanması (Binding)
                binding.apply {
                    actorImage.loadImage(mainImg)

                    actorName.text = item.actor.name
                    item.role?.let {
                        actorExtra.context?.getString(
                            when (it) {
                                ActorRole.Main -> R.string.actor_main
                                ActorRole.Supporting -> R.string.actor_supporting
                                ActorRole.Background -> R.string.actor_background
                            }
                        )?.let { text ->
                            actorExtra.isVisible = true
                            actorExtra.text = text
                        }
                    } ?: item.roleString?.let {
                        actorExtra.isVisible = true
                        actorExtra.text = it
                    } ?: run {
                        actorExtra.isVisible = false
                    }

                    if (item.voiceActor == null) {
                        voiceActorImageHolder.isVisible = false
                        voiceActorName.isVisible = false
                    } else {
                        voiceActorName.text = item.voiceActor?.name
                        if (!vaImage.isNullOrEmpty())
                            voiceActorImageHolder.isVisible = true
                        voiceActorImage.loadImage(vaImage)
                    }
                }
            }
        }
    }
}
