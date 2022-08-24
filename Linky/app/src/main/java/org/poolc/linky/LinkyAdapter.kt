package org.poolc.linky

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.poolc.linky.databinding.FolderItemSubBinding
import org.poolc.linky.databinding.LinkyItemBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LinkyAdapter (private val links:ArrayList<Link>, private val listener:LinkyAdapter.OnItemClickListener, private val isEditMode:Boolean) : RecyclerView.Adapter<LinkyAdapter.ViewHolder>() {

    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LinkyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(parent, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return links.size
    }

    inner class ViewHolder(val parent: ViewGroup, val binding : LinkyItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                var bitmap : Bitmap? = null
                thread {
                    val image = links[pos].getImgUrl()
                    if(image != null) {
                        val imageUrl: URL? = URL(image)
                        val conn: HttpURLConnection? =
                            imageUrl?.openConnection() as HttpURLConnection
                        bitmap =
                            BitmapFactory.decodeStream(conn?.inputStream)
                    }

                    if(isEditMode) {
                        val context = parent.context as EditActivity

                        if(bitmap != null) {
                            context.runOnUiThread {
                                linkyImage.setImageBitmap(bitmap)
                            }
                        }
                    }
                    else {
                        val context = parent.context as MainActivity

                        if(bitmap != null) {
                            context.runOnUiThread {
                                linkyImage.setImageBitmap(bitmap)
                            }
                        }
                    }
                }

                linkyTitle.text = links[pos].getLinkTitle()
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
                linkyKeywords.text = keywordStr

                linkyContainer.setOnLongClickListener {
                    linkyTitle.isSelected = false
                    linkyTitle.isSelected = true
                    linkyKeywords.isSelected = false
                    linkyKeywords.isSelected = true
                    true
                }

                linkyContainer.setOnClickListener {
                    listener.onItemClick(pos)
                }

                if(isEditMode) {
                    selectLink.visibility = View.VISIBLE
                    selectLink.isSelected = links[pos].getIsSelected()
                }
                else {
                    selectLink.visibility = View.INVISIBLE
                }
            }
        }
    }
}