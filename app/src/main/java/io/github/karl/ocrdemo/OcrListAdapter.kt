package io.github.karl.ocrdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class OcrListAdapter(
    private val ocrItemListener: OcrItemListener
) : ListAdapter<OcrItem, OcrListAdapter.ViewHolder>(OcrDiffCallBack()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.ocr_result_text)
        val scoreView: TextView = view.findViewById(R.id.ocr_result_score)
    }

    var selectPosition = -1
    var isClick: Boolean = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ocr_row_item, parent, false)
        return ViewHolder(view).apply {
            view.setOnClickListener {
                refreshClickItem(this.adapterPosition)
                onClick(this.adapterPosition)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ocrItem = getItem(position)
        holder.textView.text = ocrItem.text
        holder.textView.setTextColor(ocrItem.color.toArgb())
        holder.scoreView.text = ocrItem.score.toString()
        holder.scoreView.setTextColor(ocrItem.color.toArgb())

        if (selectPosition == position && isClick) {
            holder.itemView.setBackgroundResource(R.color.select_background)
        } else {
            holder.itemView.setBackgroundResource(R.color.white)
        }
    }

    private fun onClick(position: Int) =
        ocrItemListener.onClick(position, getItem(position))

    fun linkageClick(position: Int) {
        refreshClickItem(position)
    }

    private fun refreshClickItem(position: Int) {
        isClick = if (!isClick) {
            true
        } else {
            selectPosition != position
        }
        notifyItemChanged(selectPosition)
        selectPosition = position
        notifyItemChanged(selectPosition)
    }

    class OcrDiffCallBack : DiffUtil.ItemCallback<OcrItem>() {
        override fun areItemsTheSame(
            oldItem: OcrItem,
            newItem: OcrItem
        ): Boolean {
            return oldItem.text == newItem.text
        }

        override fun areContentsTheSame(
            oldItem: OcrItem,
            newItem: OcrItem
        ): Boolean {
            return oldItem == newItem
        }

    }
}