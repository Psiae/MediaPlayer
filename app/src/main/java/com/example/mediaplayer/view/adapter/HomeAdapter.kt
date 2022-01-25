package com.example.mediaplayer.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ItemHomeBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.remote.testImageUrl
import com.example.mediaplayer.util.diffSongCallback
import timber.log.Timber

class HomeAdapter(
    private val glide: RequestManager,
    private val context: Context
): RecyclerView.Adapter<HomeAdapter.HomeViewHolder>()  {

    val differ = AsyncListDiffer(this, diffSongCallback)

    var itemList: List<Song>
        get() = differ.currentList
        set(value) {
            val submit = value.distinct()
            differ.submitList(submit)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        return HomeViewHolder(ItemHomeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = itemList[position]
        holder.bindItems(item)
    }

    override fun getItemCount(): Int {
        return if (itemList.size <= 20) itemList.size else 20
    }

    inner class HomeViewHolder(
        private val binding: ItemHomeBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bindItems(item: Song) {
            val title = item.title
            val imageUri = item.imageUri
            val artist = item.artist
            val album = item.album

            Timber.d("uri: $imageUri")
            binding.apply {
                mtvTitle.text = title

                glide.asDrawable()
                    .load(imageUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_music_library_transparent)
                    .into(sivItemImage)
            }
        }
    }
}