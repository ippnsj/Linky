package org.poolc.linky

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FolderItemSubBinding

class FolderSubAdapter (private val folders:ArrayList<Folder>, private val listener: FolderSubAdapter.OnItemClickListener, private val isEditMode:Boolean) : RecyclerView.Adapter<FolderSubAdapter.ViewHolder>() {

    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
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
                folderNameSub.isSelected = false

                folderNameSub.text = folders[pos].getFolderName()
                folderContainerSub.setOnLongClickListener {
                    folderNameSub.isSelected = false
                    folderNameSub.isSelected = true
                    true
                }
                folderContainerSub.setOnClickListener {
                    listener.onItemClick(pos)
                }

                if(isEditMode) {
                    selectSub.visibility = View.VISIBLE
                    selectSub.isSelected = folders[pos].getIsSelected()
                }
                else {
                    selectSub.visibility = View.INVISIBLE
                }
            }
        }
    }
}