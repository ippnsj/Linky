package org.poolc.linky

import android.app.Activity
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
    private lateinit var context: Activity
    private lateinit var app:MyApplication

    public interface OnItemClickListener {
        fun onItemClick(pos:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LinkyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context as Activity
        app = context.application as MyApplication
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return links.size
    }

    inner class ViewHolder(val binding : LinkyItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                val imageUrl = links[pos].getImgUrl()
                if(imageUrl != "") {
                    thread {
                        val image = app.getImageUrl(imageUrl)

                        if(image != null) {
                            context.runOnUiThread {
                                linkyImage.setImageBitmap(image)
                            }
                        } else {
                            context.runOnUiThread {
                                linkyImage.setImageResource(R.mipmap.linky_logo)
                            }
                        }
                    }
                }
                else {
                    linkyImage.setImageResource(R.mipmap.linky_logo)
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

                linkyTitle.isSelected = false
                linkyKeywords.isSelected = false

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