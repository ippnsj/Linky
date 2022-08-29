package org.poolc.linky

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.ProfileItemBinding

class FriendAdapter(private val friends:ArrayList<User>, private val listener: FriendAdapter.OnItemClickListener) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {
    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    inner class ViewHolder(val binding:ProfileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                val imageUrl = friends[pos].getImageUrl()
                if(imageUrl != "") {

                }

                val nickname = friends[pos].getNickname()
                friendNickname.text = nickname

                friendContainer.setOnClickListener {
                    listener.onItemClick(pos)
                }
            }
        }
    }
}