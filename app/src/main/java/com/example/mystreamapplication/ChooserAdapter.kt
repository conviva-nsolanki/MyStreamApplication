package com.example.mystreamapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.conviva.sdk.ConvivaSdkConstants
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
            binding.btnChooser.setOnClickListener {
                println("nannandenden clicked! $item")
                callback.invoke(position)
                VideoAnalytics.initialize(binding.root.context)
                VideoAnalytics.setContentInfo(
                    mapOf(
                        ConvivaSdkConstants.ASSET_NAME to (item),
                        ConvivaSdkConstants.VIEWER_ID to "test_viewer_id",
                        ConvivaSdkConstants.IS_LIVE to false,
                        ConvivaSdkConstants.PLAYER_NAME to "Android",
                        "Custom Business Info" to "custom"
                    )
                )
                VideoAnalytics.setPlayerInfo()
                VideoAnalytics.reportPlaybackRequested()
                VideoAnalytics.initAdsSession(binding.root.context)
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