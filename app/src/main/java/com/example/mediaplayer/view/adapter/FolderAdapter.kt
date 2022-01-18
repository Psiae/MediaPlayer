package com.example.mediaplayer.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.mediaplayer.databinding.ItemFolderBinding
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.util.diffFolderCallback
import javax.inject.Inject

class FolderAdapter (
    private val glide: RequestManager,
    private val context: Context
): RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private val differ = AsyncListDiffer(this, diffFolderCallback)

    var folderList: List<Folder>
        get() = differ.currentList
        set(value) {

            val submit = value
                .sortedBy { it.title }
            differ.submitList(submit)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        return FolderViewHolder(ItemFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]
        holder.bindItem(folder)
    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    inner class FolderViewHolder (
        private val binding: ItemFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindItem(item: Folder) {
            val title = item.title
            val size = item.size
            val path = item.path

            binding.apply {
                tvTitle.text = title
                tvSecondaryTitle.text = "$size Files"
                root.setOnClickListener {
                    onFolderClickListener?.let { click ->
                        click(item)
                    }
                }
            }
        }
    }

    var onFolderClickListener: ( (Folder) -> Unit)? = null

    fun setOnFolderClicked(listener: ( (Folder) -> Unit )) {
        onFolderClickListener = listener
    }
}