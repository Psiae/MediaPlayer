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
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.util.VersionHelper
import com.example.mediaplayer.util.diffArtistCallback
import com.google.android.material.shape.CornerFamily
import kotlin.math.floor

class ArtistAdapter(
    private val glide: RequestManager,
    private val context: Context
): RecyclerView.Adapter<ArtistAdapter.HomeViewHolder>()  {

    val differ = AsyncListDiffer(this, diffArtistCallback)

    var itemList: List<Artist>
        get() = differ.currentList
        set(value) {
            val submit = value.sortedBy { it.name.lowercase() }.distinct()
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
        return itemList.size
    }

    inner class HomeViewHolder(
        private val binding: ItemHomeBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bindItems(item: Artist) {
            val name = item.name
            val song = item.song
            val firstArtist = song.first { it.artist == name }
            binding.apply {
                val cornersize = if (VersionHelper.isNougat()) 220F else 360F

                mtvTitle.text = name
                cardView.shapeAppearanceModel =
                    cardView.shapeAppearanceModel.toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, cornersize)
                        .build()

                glide.asDrawable()
                    .load(firstArtist.imageUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_music_library_transparent)
                    .into(sivItemImage)
            }
        }
    }
}