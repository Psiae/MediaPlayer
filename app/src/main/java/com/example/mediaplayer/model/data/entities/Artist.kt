package com.example.mediaplayer.model.data.entities

data class Artist(
    val name: String,
    val song: List<Song>
)

data class artistAlbum(
    val name: String,
    val album: List<Album>
)