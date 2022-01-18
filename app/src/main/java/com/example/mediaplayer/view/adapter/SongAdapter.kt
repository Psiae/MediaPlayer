package com.example.mediaplayer.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ItemSongBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.remote.testImageUrl
import com.example.mediaplayer.util.diffSongCallback
import com.example.mediaplayer.util.ext.toast
import com.google.android.material.imageview.ShapeableImageView
import timber.log.Timber
import javax.inject.Inject

class SongAdapter (
    private val glide: RequestManager,
    private val context: Context
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val differ = AsyncListDiffer(this@SongAdapter, diffSongCallback)

    var songList: List<Song>
        get() = differ.currentList
        set(value) {
            val submit = value.distinct()
            for (i in submit.indices) {
                if (submit[i].title.isEmpty()) {
                    submit[i].title = "Unknown"
                }
            }
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
            val albumId: Long = song.albumId
            val dateAdded: Int = song.dateAdded
            val isLocal: Boolean = song.isLocal
            val imageUri: String = song.imageUri
            val length: Long = song.length
            val mediaId: Long = song.mediaId
            val path: String = song.mediaPath
            val startFrom: Int = song.startFrom
            val title: String = song.title
            val year: Int = song.year

            val animation = android.view.animation.AnimationUtils.loadAnimation(this@SongAdapter.context, R.anim.anim_slidein_left)
            val bullet = 0x2022.toChar()
            binding.run {
                root.startAnimation(animation)
                root.setOnClickListener {
                    onSongItemClickListener?.let { click ->
                        click(song)
                    }
                    toast(context,
                        msg = path
                    )
                }

                if (imageUri.isEmpty()) {
                    if (artist.lowercase() == "rei"
                        || album.lowercase() == "romancer"
                        || album.lowercase() == "summit") {
                        glide.load(testImageUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerInside()
                            .placeholder(R.drawable.splash_image_24_dark)
                            .into(ivSongImage)
                    } else glide.load(R.drawable.splash_image_24_trasparent)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerInside()
                            .into(ivSongImage)
                } else {
                    glide.load(imageUri)
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

    var onSongItemClickListener: ( (Song) -> Unit )? = null

    fun setOnSongClickListener(listener: (Song) -> Unit ) {
        onSongItemClickListener = listener
    }
}