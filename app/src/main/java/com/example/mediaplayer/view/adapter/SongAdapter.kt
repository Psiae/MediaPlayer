package com.example.mediaplayer.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentSongBinding
import com.example.mediaplayer.databinding.ItemSongBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.diffSongCallback
import com.example.mediaplayer.view.activity.MainActivity
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide: RequestManager,
    private val context: Context
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val differ = AsyncListDiffer(this@SongAdapter, diffSongCallback)

    var songList: List<Song>
        get() = differ.currentList
        set(value) {
            val submit = value.distinct()
            differ.submitList(submit)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        holder.bindItem(song)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    inner class SongViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindItem (song: Song) {
            val artist: String = song.artist
            val album: String = song.album
            val albumId: Int = song.albumId
            val dateAdded: Int = song.dateAdded
            val isLocal: Boolean = song.isLocal
            val imageUri: String = song.imageUri
            val location: String = song.mediaPath
            val length: Double = song.length
            val mediaId: String = song.mediaId
            val startFrom: Double = song.startFrom
            val title: String = song.title
            val mediaUri: String = song.mediaUri
            val year: Int = song.year

            val bullet = 0x2022.toChar()
            binding.run {
                root.setOnClickListener {
                    Toast.makeText(context,
                        "${song.title} ${song.artist} ${song.album}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (imageUri.isNotEmpty()) {
                    glide.load(imageUri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerInside()
                        .into(ivSongImage)
                } else {
                    glide.load(R.drawable.ic_player_24)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerInside()
                        .into(ivSongImage)
                }
            }

            binding.apply {
                ivSongImage.visibility =
                    if (song.artist.isEmpty() && song.album.isEmpty()) View.GONE else View.VISIBLE
                tvTitle.text = if (title.isNotEmpty()) title else "Unknown"
                tvSecondaryTitle.text =
                    "${if (artist.isNotEmpty()) artist else "<Artist>"} $bullet ${if (album.isNotEmpty()) album else "<Album>"}"
            }
        }
    }
}