package org.poolc.linky

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.poolc.linky.databinding.FolderItemSubBinding
import org.poolc.linky.databinding.LinkyItemBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LinkyAdapter (private val linkys:ArrayList<HashMap<String, Any>>) : RecyclerView.Adapter<LinkyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LinkyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(parent, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return linkys.size
    }

    inner class ViewHolder(val parent: ViewGroup, val binding : LinkyItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos:Int) {
            with(binding) {
                var bitmap : Bitmap? = null
                thread {
                    val image = linkys[pos]["imgUrl"].toString()
                    if(image != null) {
                        val imageUrl: URL? = URL(image)
                        val conn: HttpURLConnection? =
                            imageUrl?.openConnection() as HttpURLConnection
                        bitmap =
                            BitmapFactory.decodeStream(conn?.inputStream)
                    }

                    val context = parent.context as MainActivity
                    if(bitmap != null) {
                        context.runOnUiThread {
                            linkyImage.setImageBitmap(bitmap)
                        }
                    }
                }

                linkyTitle.text = linkys[pos]["title"].toString()
                val keywordsArr = linkys[pos]["keywords"] as JSONArray
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
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkys[pos]["url"].toString()))
                    parent.context.startActivity(intent)
                }
            }
        }
    }
}