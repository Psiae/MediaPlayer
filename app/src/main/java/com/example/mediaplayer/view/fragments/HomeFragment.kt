package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentHomeBinding
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        val TAG = HomeFragment::class.java.simpleName
    }

    @Inject
    lateinit var player: ExoPlayer

    lateinit var navController: NavController
    val songViewModel: SongViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Timber.d("HomeFragment Inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = requireActivity().findNavController(R.id.navHostContainer)
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("HomeFragment Destroyed")
    }

}