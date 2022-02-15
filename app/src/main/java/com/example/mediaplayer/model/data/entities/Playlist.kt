package com.example.mediaplayer.model.data.entities

data class Playlist(
    val description: String = "",
    val imageUri: String = "",
    val songList: List<Song>,
    val title: String = "",
)