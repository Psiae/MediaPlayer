package com.example.mediaplayer.model.data.entities

data class SongAlbumArtist(
    val album: String,
    val artist: String,
    val song: List<Song>
)