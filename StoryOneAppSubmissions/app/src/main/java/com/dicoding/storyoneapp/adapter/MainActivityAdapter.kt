package com.dicoding.storyoneapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.storyoneapp.R
import com.dicoding.storyoneapp.data.response.ListStoryItem
import com.dicoding.storyoneapp.databinding.ItemStoryBinding
import com.dicoding.storyoneapp.ui.detail.DetailActivity

class MainActivityAdapter :
    PagingDataAdapter<ListStoryItem, MainActivityAdapter.StoryViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListStoryItem>() {
            override fun areItemsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = getItem(position)
        if (story != null) {
            holder.bind(story)
        }
    }

    class StoryViewHolder(private val binding: ItemStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(story: ListStoryItem) {
            // Set data ke elemen UI
            binding.tvItemTitle.text = story.name
            binding.tvItemDescription.text = story.description
            binding.tvItemDate.text = story.createdAt

            // Tampilkan gambar menggunakan Glide
            Glide.with(binding.ivPhoto.context)
                .load(story.photoUrl)
                .placeholder(R.drawable.ic_placeholder) // Gambar placeholder
                .error(R.drawable.ic_error) // Gambar error
                .into(binding.ivPhoto)

            // Tambahkan klik listener untuk navigasi ke DetailActivity
            binding.root.setOnClickListener {
                val intent = Intent(binding.root.context, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_DATA, story) // Pass the object using Serializable
                binding.root.context.startActivity(intent)
            }


        }
    }
}
