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

    // Easier to store it here than to store it in the ActorData
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

                // Fix tv focus escaping the recyclerview
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
                // 1. PENCERE: NORMAL KISA TIKLAMA (OK Tuşu / Ekrana Dokunma)
                // NOT: Hem biyografi hem de oynadığı film/dizileri gösteren alt sayfayı (ActorFilmography) açar.
                // =========================================================================
                itemView.setOnClickListener {
                    ActorFilmography.show(itemView.context, item.voiceActor ?: item.actor)
                }

                // =========================================================================
                // 2. PENCERE: UZUN BASMA (OK Tuşuna Basılı Tutma)
                // NOT: Sadece oyuncunun biyografisini gösteren küçük pop-up penceresini (ActorInfoDialog) açar.
                // =========================================================================
                itemView.setOnLongClickListener {
                    ActorInfoDialog.show(itemView.context, item.voiceActor ?: item.actor)
                    true
                }

                // =========================================================================
                // 3. PENCERE: TV KUMANDASI VEYA TKLAMA İLE ÖZEL TUŞ BASIMI
                // NOT: Kumandadaki Menü, Sarı, Mavi veya Bilgi (Info) tuşuna basıldığında tetiklenir.
                // =========================================================================
                itemView.setOnKeyListener { view, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            // İstediğiniz kumanda tuş kodlarını buraya ekleyebilirsiniz:
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,  // Ortadaki Oynat/Durdur tuşu
                            KeyEvent.KEYCODE_MEDIA_PLAY,        // Sadece Oynat tuşu olan kumandalar için
                            KeyEvent.KEYCODE_MEDIA_PAUSE,          // Kumanda Menü tuşu
                            KeyEvent.KEYCODE_PROG_YELLOW,   // Kumanda Sarı tuş
                            KeyEvent.KEYCODE_INFO -> {      // Kumanda Bilgi (Info) tuşu
                                
               itemView.setOnKeyListener { view, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            // İstediğiniz kumanda tuş kodlarını buraya ekleyebilirsiniz:
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,  // Ortadaki Oynat/Durdur tuşu
                            KeyEvent.KEYCODE_MEDIA_PLAY,        // Sadece Oynat tuşu olan kumandalar için
                            KeyEvent.KEYCODE_MEDIA_PAUSE,          // Kumanda Menü tuşu
                            KeyEvent.KEYCODE_PROG_YELLOW,   // Kumanda Sarı tuş
                            KeyEvent.KEYCODE_INFO -> {      // Kumanda Bilgi (Info) tuşu
                                
               
                fun pushSearch(autoSearch: String? = null, providers: Array<String>? = null)
                )
                                
                                true // Tuş olayının işlendiğini belirtir.
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                                
                                true // Tuş olayının işlendiğini belirtir.
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }

                binding.apply {
                    actorImage.loadImage(mainImg)

                    actorName.text = item.actor.name
                    item.role?.let {
                        actorExtra.context?.getString(
                            when (it) {
                                ActorRole.Main -> {
                                    R.string.actor_main
                                }

                                ActorRole.Supporting -> {
                                    R.string.actor_supporting
                                }

                                ActorRole.Background -> {
                                    R.string.actor_background
                                }
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
