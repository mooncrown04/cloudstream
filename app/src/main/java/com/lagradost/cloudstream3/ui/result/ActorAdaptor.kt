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

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class ActorAdaptor(
    private var nextFocusUpId: Int? = null,
    private val focusCallback: (View?) -> Unit = {},
    private val searchCallback: (String) -> Unit = {} // ← YENİ: Arama callback'i
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    data class ActorMetaData(
        var isInverted: Boolean,
        val actor: ActorData,
    )

    private val actors: MutableList<ActorMetaData> = mutableListOf()

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CardViewHolder(
            CastItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CardViewHolder -> {
                holder.bind(actors[position].actor, actors[position].isInverted, position) {
                    actors[position].isInverted = !actors[position].isInverted
                    this.notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return actors.size
    }

    private fun updateActorList(newList: List<ActorMetaData>) {
        val diffResult = DiffUtil.calculateDiff(
            ActorDiffCallback(this.actors, newList)
        )

        actors.clear()
        actors.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    fun updateList(newList: List<ActorData>) {
        if (actors.size >= newList.size) {
            updateActorList(newList.mapIndexed { i, data -> actors[i].copy(actor = data) })
        } else {
            updateActorList(newList.mapIndexed { i, data ->
                if (i < actors.size)
                    actors[i].copy(actor = data)
                else ActorMetaData(isInverted = false, actor = data)
            })
        }
    }

    private inner class CardViewHolder(
        val binding: CastItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(actor: ActorData, isInverted: Boolean, position: Int, callback: (Int) -> Unit) {
            val (mainImg, vaImage) = if (!isInverted || actor.voiceActor?.image.isNullOrBlank()) {
                Pair(actor.actor.image, actor.voiceActor?.image)
            } else {
                Pair(actor.voiceActor?.image, actor.actor.image)
            }

        // --- TV NAVİGASYON VE ODAKLANMA (FOCUS) AYARLARI ---
        // Listenin en solunda veya en sağında kumandanın takılı kalmasını önler
        if (position == 0) {
            itemView.nextFocusLeftId = R.id.result_cast_items
        } else if (position == itemCount - 1) {
            itemView.nextFocusRightId = R.id.result_cast_items
        }
        
        // Üstteki butonlara (Örn: Arama butonu) geri dönebilmek için
        nextFocusUpId?.let { itemView.nextFocusUpId = it }

        // Oyuncunun üzerine gelindiğinde (Focus olduğunda) çalışır
        itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) focusCallback(v)
        }

        // --- TIKLAMA İŞLEMİ (ARAMA YAPMA) ---
        itemView.setOnClickListener {
            // 1. Fragment'taki MainActivity arama intent'ini tetikler
            searchCallback.invoke(actor.actor.name)
            
            // 2. Resimlerin yerini değiştirme (İnvert) mantığını korur
            callback(position)
        }

        // --- UZUN BASMA (TELEFONLAR İÇİN WEB ARAMASI) ---
        itemView.setOnLongClickListener {
            if (isLayout(PHONE)) {
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, actor.actor.name)
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

        // --- GÖRSEL VE METİN ATAMALARI ---
        binding.apply {
            actorImage.loadImage(mainImg)
            actorName.text = actor.actor.name
            actor.role?.let {
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
            } ?: actor.roleString?.let {
                actorExtra.isVisible = true
                actorExtra.text = it
            } ?: run {
                actorExtra.isVisible = false
            }

            // Seslendirme Sanatçısı Bilgisi (Varsa)
            if (actor.voiceActor == null) {
                voiceActorImageHolder.isVisible = false
                voiceActorName.isVisible = false
            } else {
                voiceActorName.text = actor.voiceActor?.name
                if (!vaImage.isNullOrEmpty()) {
                    voiceActorImageHolder.isVisible = true
                    voiceActorImage.loadImage(vaImage)
                }
            }
        }
    }
}

}

class ActorDiffCallback(
    private val oldList: List<ActorAdaptor.ActorMetaData>,
    private val newList: List<ActorAdaptor.ActorMetaData>
) :
    DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].actor.actor.name == newList[newItemPosition].actor.actor.name

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]}

