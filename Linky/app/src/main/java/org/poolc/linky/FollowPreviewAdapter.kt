package org.poolc.linky

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FollowPreviewItemBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FollowPreviewAdapter(private val follows:ArrayList<User>, private val listener: FollowPreviewAdapter.OnItemClickListener) : RecyclerView.Adapter<FollowPreviewAdapter.ViewHolder>() {
    private lateinit var context: MainActivity
    private lateinit var app:MyApplication

    interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FollowPreviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class ViewHolder(val binding:FollowPreviewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                val imageUrl = follows[pos].getImageUrl()
                if(imageUrl != "") {
                    thread {
                        val image = app.getImageUrl(imageUrl)
                        if (image != null) {
                            context.runOnUiThread {
                                followPreviewProfileImage.setImageBitmap(image)
                            }
                        }
                        else {
                            context.runOnUiThread {
                                followPreviewProfileImage.setImageResource(R.drawable.profile)
                            }
                        }
                    }
                }
                else {
                    followPreviewProfileImage.setImageResource(R.drawable.profile)
                }

                val following = follows[pos].getFollowing()
                if(following) {
                    followPreviewProfileImage.borderColor = Color.parseColor("#D6F9F7")
                    followPreviewProfileImage.borderWidth = 7
                }

                val nickname = follows[pos].getNickname()
                followPreviewNickname.text = nickname

                followPreviewContainer.setOnClickListener {
                    listener.onItemClick(pos)
                }
            }
        }
    }
}