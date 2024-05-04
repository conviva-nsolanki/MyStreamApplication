package com.example.mystreamapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.mystreamapplication.databinding.ItemChooserBinding

class ChooserAdapter(private val callback: (Int) -> Unit): ListAdapter<String, ChooserAdapter.StartPlayViewHolder>(ChooserItemDiffCallback) {

    private val items = arrayListOf<String>()
    fun setData(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        submitList(items)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartPlayViewHolder {
        val binding = ItemChooserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StartPlayViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: StartPlayViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position, callback)
    }
    class StartPlayViewHolder(private val binding: ItemChooserBinding): ViewHolder(binding.root) {
        fun bind(item: String, position: Int, callback: (Int) -> Unit) {
            binding.btnChooser.text = item
            println("nannandenden $item")
            binding.btnChooser.setOnClickListener {
                callback.invoke(position)
            }
        }
    }
}

object ChooserItemDiffCallback: DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

}