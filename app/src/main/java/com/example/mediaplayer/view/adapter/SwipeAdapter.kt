package com.example.mediaplayer.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.example.mediaplayer.databinding.ItemSwipeBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.diffSongCallback
import com.example.mediaplayer.util.ext.toast
import javax.inject.Inject

class SwipeAdapter @Inject constructor(
    private val context: Context
): RecyclerView.Adapter<SwipeAdapter.SwipeViewHolder>()  {

    val differ = AsyncListDiffer<Song>(this, diffSongCallback)

    var songList : List<Song>
        get() = differ.currentList
        set(value) {
            val submit = value.distinct()
            differ.submitList(submit)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        return SwipeViewHolder(ItemSwipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
        val item = songList[position]
        holder.bindItems(item)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    inner class SwipeViewHolder(
        private val binding: ItemSwipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(item: Song) {
            binding.apply {
                val bullet = 0x2022.toChar()
                tvPrimary.text = item.title
                tvSecondary.text = if (item.artist.isNotEmpty()) item.artist else item.album
                root.setOnClickListener {
                    itemClickListener?.let { click ->
                        click(item)
                    } ?: toast(context, item.title)
                }
            }
        }
    }

    private var itemClickListener: ( (Song) -> Unit )? = null

    fun setItemListener(listener: (Song) -> Unit ) {
        itemClickListener = listener
    }
}