package com.example.mediaplayer.view.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ItemPlayingBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.diffSongCallback

class PlayingAdapter(
    private val glide: RequestManager,
    private val context: Context
) : RecyclerView.Adapter<PlayingAdapter.PlayingViewHolder>() {

    val differ = AsyncListDiffer(this, diffSongCallback)

    var songList: List<Song>
        get() = differ.currentList
        set(value) {
            val submit = value.distinct()
            differ.submitList(submit)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayingViewHolder {
        return PlayingViewHolder(ItemPlayingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: PlayingViewHolder, position: Int) {
        val item = songList[position]
        holder.bindItems(item)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    inner class PlayingViewHolder(
        private val binding: ItemPlayingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindItems(item: Song) {
            binding.apply {
                glide.asDrawable()
                    .load(item.imageUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_music_library_transparent)
                    .into(sivItemImage)

                root.setOnClickListener {
                    itemClickListener?.let {
                        it(item)
                    }
                }
            }
        }
    }

    private var itemClickListener: ( (Song) -> Unit )? = null

    fun setClickListener(listener: ( (Song) -> Unit)) {
        itemClickListener = listener
    }
}