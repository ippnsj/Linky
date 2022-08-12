package org.poolc.linky

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.KeywordBubbleBinding

class KeywordAdapter(private val keywords:ArrayList<String>, private val listener:OnItemClickListener) : RecyclerView.Adapter<KeywordAdapter.ViewHolder>() {
    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordAdapter.ViewHolder {
        val binding = KeywordBubbleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return keywords.size
    }

    inner class ViewHolder(val binding: KeywordBubbleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                keyword.text = keywords[pos]
                keywordLayout.setOnClickListener {
                    listener.onItemClick(adapterPosition)
                }
            }
        }
    }
}