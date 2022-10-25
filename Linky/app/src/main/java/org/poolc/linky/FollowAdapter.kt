package org.poolc.linky

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FollowItemBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FollowAdapter(private val follows:ArrayList<User>, private val listener: FollowAdapter.OnItemClickListener) : RecyclerView.Adapter<FollowAdapter.ViewHolder>() {
    private lateinit var context: MainActivity
    private lateinit var app:MyApplication

    interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FollowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context as MainActivity
        app = context.application as MyApplication
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return follows.size
    }

    inner class ViewHolder(val binding: FollowItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                val imageUrl = follows[pos].getImageUrl()
                if(imageUrl != "") {
                    thread {
                        val image = app.getImageUrl(imageUrl)
                        if(image != null) {
                            context.runOnUiThread {
                                followProfileImage.setImageBitmap(image)
                            }
                        }
                        else {
                            context.runOnUiThread {
                                followProfileImage.setImageResource(R.drawable.profile)
                            }
                        }
                    }
                }else {
                    followProfileImage.setImageResource(R.drawable.profile)
                }

                val following = follows[pos].getFollowing()
                if(following) {
                    followProfileImage.borderColor = context.getColor(R.color.primary)
                    followProfileImage.borderWidth = 7
                }
                else {
                    followProfileImage.borderWidth = 0
                }

                val nickname = follows[pos].getNickname()
                followNickname.text = nickname

                followContainer.setOnClickListener {
                    listener.onItemClick(pos)
                }
            }
        }
    }
}