package com.lagradost.cloudstream3.ui.result

import android.app.SearchManager
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.ActorRole
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.CastItemBinding
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.NoStateAdapter
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.newSharedPool
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage

class ActorAdaptor(
    private var nextFocusUpId: Int? = null,
    private val focusCallback: (View?) -> Unit = {},
    private val searchCallback: (String) -> Unit = {} // SENİN EKLEDİĞİN ARAMA CALLBACK'İ
) : NoStateAdapter<ActorData>(diffCallback = BaseDiffCallback(itemSame = { a, b ->
    a.actor.name == b.actor.name
})) {
    companion object {
        val sharedPool =
            newSharedPool { setMaxRecycledViews(CONTENT, 10) }
    }

    val inverted: HashMap<ActorData, Boolean> = hashMapOf()

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        return ViewHolderState(
            CastItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onClearView(holder: ViewHolderState<Any>) {
        when (val binding = holder.view) {
            is CastItemBinding -> {
                // clearImage varsa burada kalsın, yoksa derleme hatası verirse silebilirsin
                // clearImage(binding.actorImage) 
            }
        }
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

                // Focus Ayarları
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

                // TIKLAMA OLAYI (SENİN İSTEDİĞİN ARAMA ÖZELLİĞİ BURADA)
                itemView.setOnClickListener {
                    val actorName = item.actor.name
                    if (!actorName.isNullOrBlank()) {
                        searchCallback(actorName) // Aktör ismini arama fonksiyonuna gönderir
                    }
                    
                    // Resim değiştirme (inverted) mantığını korumak istersen:
                    inverted[item] = !isInverted
                    this.onUpdateContent(holder, getItem(position), position)
                }

                // Uzun Basma (Telefonlar için Web araması)
                itemView.setOnLongClickListener {
                    if (isLayout(PHONE)) {
                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, item.actor.name)
                        }.also { intent ->
                            itemView.context.packageManager?.let { pm ->
                                if (intent.resolveActivity(pm) != null) {
                                    itemView.context.startActivity(intent)
                                }
                            }
                        }
                    }
                    true
                }

                binding.apply {
                    actorImage.loadImage(mainImg)
                    actorName.text = item.actor.name
                    
                    // Rol gösterimi
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

                    // Seslendirme Sanatçısı
                    if (item.voiceActor == null) {
                        voiceActorImageHolder.isVisible = false
                        voiceActorName.isVisible = false
                    } else {
                        voiceActorName.text = item.voiceActor?.name
                        voiceActorImageHolder.isVisible = !vaImage.isNullOrEmpty()
                        voiceActorImage.loadImage(vaImage)
                    }
                }
            }
        }
    }
}
