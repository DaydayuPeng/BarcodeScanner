package com.tugulu.scanner.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tugulu.scanner.data.ScanRecord
import com.tugulu.scanner.databinding.ItemRecordBinding

class RecordAdapter(
    private val onDelete: (ScanRecord) -> Unit
) : RecyclerView.Adapter<RecordAdapter.VH>() {

    private val items = mutableListOf<ScanRecord>()

    fun submit(list: List<ScanRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun notifyPhotoUpdated(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScanRecord) {
            binding.tvTracking.text = item.trackingNo
            if (!item.photoPath.isNullOrBlank()) {
                val bmp = BitmapFactory.decodeFile(item.photoPath)
                if (bmp != null) {
                    binding.ivThumb.setImageBitmap(bmp)
                    binding.ivThumb.visibility = View.VISIBLE
                } else {
                    binding.ivThumb.setImageDrawable(null)
                }
            } else {
                binding.ivThumb.setImageDrawable(null)
            }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
