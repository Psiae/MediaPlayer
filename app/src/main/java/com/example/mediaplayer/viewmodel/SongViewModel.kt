package com.example.mediaplayer.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mediaplayer.model.data.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    // Inject
) : ViewModel() {

    private val _songList = MutableLiveData<List<Song>>()
    val songList: MutableLiveData<List<Song>>
        get() = _songList


    fun postSongList(list: List<Song>) {
        _songList.value = list
    }
}