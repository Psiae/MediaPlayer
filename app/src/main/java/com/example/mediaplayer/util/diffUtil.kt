package com.example.mediaplayer.util

import androidx.recyclerview.widget.DiffUtil
import com.example.mediaplayer.model.data.entities.Song
import timber.log.Timber

val diffSongCallback = object : DiffUtil.ItemCallback<Song>() {

    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem.mediaId == newItem.mediaId
    }

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}