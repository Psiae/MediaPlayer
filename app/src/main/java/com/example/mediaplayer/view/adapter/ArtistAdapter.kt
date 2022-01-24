package com.example.mediaplayer.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.mediaplayer.databinding.ItemHomeBinding
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.util.diffArtistCallback

class ArtistAdapter(
    private val glide: RequestManager,
    private val context: Context
): RecyclerView.Adapter<ArtistAdapter.HomeViewHolder>()  {

    val differ = AsyncListDiffer(this, diffArtistCallback)

    var itemList: List<Artist>
        get() = differ.currentList
        set(value) {
            val submit = value.distinct()
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
        return if (itemList.size <= 20) itemList.size else 20
    }

    inner class HomeViewHolder(
        private val binding: ItemHomeBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bindItems(item: Artist) {
            val name = item.name
            val song = item.song

            binding.apply {
                mtvTitle.text = "$name"
            }
        }
    }
}