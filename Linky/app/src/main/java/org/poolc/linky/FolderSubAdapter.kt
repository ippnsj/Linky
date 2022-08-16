package org.poolc.linky

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FolderItemSubBinding

class FolderSubAdapter (private val folders:ArrayList<String>, private val listener: FolderSubAdapter.OnItemClickListener) : RecyclerView.Adapter<FolderSubAdapter.ViewHolder>() {

    public interface OnItemClickListener {
        fun onItemClick(folderName:String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FolderItemSubBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    inner class ViewHolder(val binding : FolderItemSubBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                folderNameSub.text = folders[pos]
                folderContainerSub.setOnLongClickListener {
                    folderNameSub.isSelected = false
                    folderNameSub.isSelected = true
                    true
                }
                folderContainerSub.setOnClickListener {
                    listener.onItemClick(folders[pos])
                }
            }
        }
    }
}