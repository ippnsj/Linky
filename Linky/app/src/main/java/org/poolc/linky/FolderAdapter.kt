package org.poolc.linky

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FolderItemBinding

class FolderAdapter(private val folders:ArrayList<String>, private val listener: FolderAdapter.OnItemClickListener) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    public interface OnItemClickListener {
        fun onItemClick(folderName:String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FolderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    inner class ViewHolder(val binding : FolderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                folderName.text = folders[pos]
                folderContainer.setOnLongClickListener {
                    folderName.isSelected = false
                    folderName.isSelected = true
                    true
                }
                folderContainer.setOnClickListener {
                    listener.onItemClick(folders[pos])
                }
            }
        }
    }
}