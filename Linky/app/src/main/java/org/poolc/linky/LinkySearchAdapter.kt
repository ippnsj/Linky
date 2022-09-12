package org.poolc.linky

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.poolc.linky.databinding.LinkyItemBinding
import org.poolc.linky.databinding.LinkyItemSearchBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LinkySearchAdapter(private val links:ArrayList<Link>, private val listener:LinkySearchAdapter.OnItemClickListener) : RecyclerView.Adapter<LinkySearchAdapter.ViewHolder>() {
    private lateinit var context: Activity

    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LinkyItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context as Activity
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return links.size
    }

    inner class ViewHolder(val binding : LinkyItemSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                var bitmap : Bitmap? = null

                val image = links[pos].getImgUrl()
                if(image != "null") {
                    thread {
                        try {
                            val imageUrl: URL? = URL(image)
                            val conn: HttpURLConnection? =
                                imageUrl?.openConnection() as HttpURLConnection
                            bitmap =
                                BitmapFactory.decodeStream(conn?.inputStream)

                            context.runOnUiThread {
                                linkImage.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            context.runOnUiThread {
                                linkImage.setImageResource(R.mipmap.linky_logo)
                            }
                            e.printStackTrace()
                        }
                    }
                }
                else {
                    linkImage.setImageResource(R.mipmap.linky_logo)
                }

                linkTitle.text = links[pos].getLinkTitle()
                val keywordsArr = links[pos].getKeywords()
                var keywordStr = ""
                for(idx in 0 until keywordsArr.length()) {
                    if(idx == 0) {
                        keywordStr += "#${keywordsArr[idx]}"
                    }
                    else {
                        keywordStr += " #${keywordsArr[idx]}"
                    }
                }
                linkKeyword.text = keywordStr

                linkTitle.isSelected = false
                linkKeyword.isSelected = false

                linkContainer.setOnLongClickListener {
                    linkTitle.isSelected = false
                    linkTitle.isSelected = true
                    linkKeyword.isSelected = false
                    linkKeyword.isSelected = true
                    true
                }

                linkContainer.setOnClickListener {
                    listener.onItemClick(pos)
                }

                linkOwnerNickname.text = "by ${links[pos].getNickname()}"

                if(links[pos].getFollowing()) {
                    linkOwnerNickname.background = ContextCompat.getDrawable(context, R.drawable.owner_nickname_background_following)
                }
                else {
                    linkOwnerNickname.background = ContextCompat.getDrawable(context, R.drawable.owner_nickname_background)
                }
            }
        }
    }
}