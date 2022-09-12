package org.poolc.linky

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FolderItemSearchBinding

class FolderSearchAdapter(private val folders:ArrayList<Folder>, private val listener: FolderSearchAdapter.OnItemClickListener) : RecyclerView.Adapter<FolderSearchAdapter.ViewHolder>(){
    private lateinit var context : MainActivity

    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FolderItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context as MainActivity
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    inner class ViewHolder(val binding : FolderItemSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos: Int) {
            with(binding) {
                searchFolderName.isSelected = false
                folderOwnerNickname.isSelected = false

                searchFolderName.text = folders[pos].getFolderName()

                folderContainer.setOnLongClickListener {
                    searchFolderName.isSelected = false
                    searchFolderName.isSelected = true
                    folderOwnerNickname.isSelected = false
                    folderOwnerNickname.isSelected = true
                    true
                }
                folderContainer.setOnClickListener {
                    listener.onItemClick(pos)
                }

                folderOwnerNickname.text = "by ${folders[pos].getNickname()}"

                if(folders[pos].getFollowing()) {
                    folderOwnerNicknameContainer.background = ContextCompat.getDrawable(context, R.drawable.owner_nickname_background_following)
                }
                else {
                    folderOwnerNicknameContainer.background = ContextCompat.getDrawable(context, R.drawable.owner_nickname_background)
                }
            }
        }
    }
}