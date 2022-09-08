package org.poolc.linky

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.FollowPreviewItemBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FollowPreviewAdapter(private val follows:ArrayList<User>, private val listener: FollowPreviewAdapter.OnItemClickListener) : RecyclerView.Adapter<FollowPreviewAdapter.ViewHolder>() {
    private lateinit var content: MainActivity

    interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FollowPreviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        content = parent.context as MainActivity
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
                        try {
                            val url: URL? = URL(imageUrl)
                            val conn: HttpURLConnection? =
                                url?.openConnection() as HttpURLConnection
                            val image = BitmapFactory.decodeStream(conn?.inputStream)

                            content.runOnUiThread {
                                followPreviewProfileImage.setImageBitmap(image)
                            }
                        }
                        catch (e:Exception) {
                            e.printStackTrace()
                        }
                    }
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