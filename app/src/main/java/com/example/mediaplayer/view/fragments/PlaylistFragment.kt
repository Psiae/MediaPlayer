package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mediaplayer.databinding.FragmentPlaylistBinding
import com.example.mediaplayer.util.Constants
import com.google.android.material.transition.MaterialFadeThrough
import timber.log.Timber

class PlaylistFragment: Fragment() {

    companion object {
        val TAG = PlaylistFragment::class.java.simpleName
    }

    private var _binding: FragmentPlaylistBinding? = null
    private val binding: FragmentPlaylistBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        Timber.d("FragmentPlaylistBinding Inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = MaterialFadeThrough().addTarget(view as ViewGroup).also {
            it.duration = Constants.FADETHROUGH_IN_DURATION
        }
        exitTransition = MaterialFadeThrough().addTarget(view).also {
            it.duration = Constants.FADETHROUGH_OUT_DURATION
        }
        binding.apply {}
    }



    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("PlaylistFragment Destroyed")
    }
}