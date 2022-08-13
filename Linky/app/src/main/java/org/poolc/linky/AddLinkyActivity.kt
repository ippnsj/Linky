package org.poolc.linky

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.jsoup.Jsoup
import org.poolc.linky.databinding.ActivityAddLinkyBinding
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern
import kotlin.concurrent.thread

class AddLinkyActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAddLinkyBinding
    private val keywords = ArrayList<String>()
    private lateinit var keywordAdapter : KeywordAdapter
    private var intentChanged = true
    private lateinit var switch : Switch
    private lateinit var folderResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var albumResultLauncher : ActivityResultLauncher<Intent>
    private val permissionList = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keywordAdapter = KeywordAdapter(keywords, object : KeywordAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val builder = AlertDialog.Builder(this@AddLinkyActivity)
                val message = "키워드 '${keywords[pos]}' 을/를 삭제하시겠습니까?"
                builder.setMessage(message)

                builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                    keywords.removeAt(pos)
                    keywordAdapter.notifyItemRemoved(pos)
                    binding.tagTextInput.error = null
                }

                builder.setNegativeButton("취소", null)

                builder.show()
            }
        })

        folderResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK) {
                val path = result.data?.getStringExtra("path")

                if(path != null) {
                    if(path == "") {
                        binding.folderPath.text = getString(R.string.default_path)
                    }
                    else {
                        binding.folderPath.text = path
                    }
                }
            }
        }

        albumResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK) {
                // 선택한 이미지의 경로 데이터를 관리하는 Uri 객체를 추출한다.
                val uri = result.data?.data

                if(uri != null) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // 안드로이드 10 버전부터
                        // 외부 저장소로부터 이미지파일을 읽으면 안드로이드 OS DB에 자동으로 해당 이미지 파일의 정보가 저장된다.
                        // DB에 저장된 정보를 사용하기 위해 Content Provider인 contentResolver를 넘겨주어야 한다.
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        val resizedBitmap = resizeBitmap(resources.displayMetrics.widthPixels - 100, bitmap)
                        binding.linkImage.setImageBitmap(resizedBitmap)
                    }
                    else {
                        // 안드로이드 9 버전까지
                        val cursor = contentResolver.query(uri, null, null, null, null)
                        if(cursor != null) {
                            cursor.moveToNext()
                            // 이미지 경로를 가져온다.
                            val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            val source = cursor.getString(index)
                            // 이미지를 생성한다.
                            val bitmap = BitmapFactory.decodeFile(source)
                            val resizedBitmap = resizeBitmap(resources.displayMetrics.widthPixels - 100, bitmap)
                            binding.linkImage.setImageBitmap(resizedBitmap)
                        }
                    }
                }
            }
        }

        with(binding) {
            // topbar 설정
            setSupportActionBar(addLinkyTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            addLinkyTopbarTitle.text = "링크 추가하기"

            // clear keywords listener 설정
            clearKeywords.setOnClickListener(clearKeywordsListener)

            // image listener 설정
            changePicture.setOnClickListener(imageListener)

            // keyword textinput 키보드 이벤트 설정
            tagTextInput.setOnKeyListener(keywordKeyListener)

            // keyword recyclerview adapter 설정
            keywordRecycler.adapter = keywordAdapter

            // keyword recyclerview item 간격 설정
            keywordRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)

                    if(pos != 0) {
                        outRect.left = 20
                    }
                }
            })

            // select path listener 설정
            selectPath.setOnClickListener(selectPathListener)
        }
    }

    override fun onRestart() {
        super.onRestart()
        currentFocus?.clearFocus()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if(intent != null) {
            setIntent(intent)
            intentChanged = true

            with(binding) {
                // 값 초기화
                switch.isChecked = false
                keywords.clear()
                keywordAdapter.notifyDataSetChanged()
                folderPath.setText(R.string.default_path)
                titleTextInput.setText("")
                linkImage.setImageResource(R.mipmap.linky_logo)
                linkAddressTextInput.setText("")
                linkAddressTextInput.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            if(intentChanged) {
                intentChanged = false

                // 공유하기로부터 온 intent 처리
                if (intent.action == Intent.ACTION_SEND) {
                    if (intent.type == "text/plain") {
                        veil.visibility = View.VISIBLE

                        val txt = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
                        var pattern = Pattern.compile(
                            "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                            Pattern.CASE_INSENSITIVE
                        )
                        var matcher = pattern.matcher(txt)
                        if (matcher.find()) {
                            var encodedUrl = txt.substring(matcher.start(0), matcher.end(0))
                            var url = URLDecoder.decode(encodedUrl, "UTF-8")

                            // 메타데이터 추출
                            thread {
                                var title : String? = null
                                var image : String? = null

                                var doc = Jsoup.connect(url).userAgent("Chrome").get()
                                if(url.contains("naver.me")) {
                                    val metaUrl : String? = doc.select("meta[property=al:android:url]").first()?.attr("content")
                                    pattern = Pattern.compile(
                                        "(url=)+[\\w\\d:#@%/;\$()~_?\\+-=\\\\\\.&]*(&version)+",
                                        Pattern.CASE_INSENSITIVE
                                    )
                                    matcher = pattern.matcher(metaUrl)
                                    if(matcher.find()) {
                                        encodedUrl = metaUrl!!.substring(matcher.start(0) + 4, matcher.end(0) - 8)
                                        url = URLDecoder.decode(encodedUrl, "UTF-8")
                                        doc = Jsoup.connect(url).userAgent("Chrome").get()
                                    }
                                }

                                val metaTags = doc.select("meta[property]")
                                for(meta in metaTags) {
                                    if(title != null && image != null) {
                                        break
                                    }

                                    if(title == null && meta.attr("property").contains("title")) {
                                        title = meta.attr("content")
                                    }

                                    if(image == null && meta.attr("property").contains("image")) {
                                        image = meta.attr("content")
                                    }
                                }

                                // TODO 키워드 제한으로 일단 삭제
                                /*val keywordsStr : String? =
                                    doc.select("meta[name=keywords]").first()?.attr("content")
                                if(keywordsStr != null) {
                                    val keywordsArr = keywordsStr.split(",")
                                    keywords.addAll(keywordsArr)
                                }*/

                                if(title == null) {
                                    title = doc.getElementsByTag("title")?.first()?.text()
                                }

                                if(image == null) {
                                    image = doc.getElementsByTag("img").first()?.absUrl("src")
                                }

                                var resizedBitmap: Bitmap? = null
                                if(image != null) {
                                    val imageUrl: URL? = URL(image)
                                    val conn: HttpURLConnection? =
                                        imageUrl?.openConnection() as HttpURLConnection
                                    val bitmap: Bitmap? =
                                        BitmapFactory.decodeStream(conn?.inputStream)
                                    if (bitmap != null) {
                                        resizedBitmap =
                                            resizeBitmap(
                                                resources.displayMetrics.widthPixels - 100,
                                                bitmap
                                            )
                                    }
                                }

                                runOnUiThread {
                                    veil.visibility = View.INVISIBLE

                                    // 키워드
                                    /*if(keywords.isNotEmpty()) {
                                        keywordAdapter.notifyDataSetChanged()
                                    }*/

                                    // 제목
                                    titleTextInput.setText(title ?: "")

                                    // 대표이미지
                                    if (resizedBitmap != null) {
                                        linkImage.setImageBitmap(resizedBitmap)
                                    }

                                    // 링크주소
                                    linkAddressTextInput.setText(url)
                                    linkAddressTextInput.isEnabled = false
                                }

                            }
                        } else {
                            Log.d("test", "NO URL")
                        }
                    }
                } else if (intent.action == Intent.ACTION_MAIN) {
                    // 앱에서 실행된 intent인 경우
                }
            }
        }
    }

    // 사진의 사이즈를 조정하는 메서드
    private fun resizeBitmap(targetWidth:Int, img: Bitmap) : Bitmap {
        // 이미지의 비율을 계산한다.
        val ratio = targetWidth.toDouble() / img.width.toDouble()
        // 보정될 세로 길이를 구한다.
        val targetHeight = (img.height * ratio).toInt()
        // 크기를 조정한 bitmap 객체를 생성한다.
        val result = Bitmap.createScaledBitmap(img, targetWidth, targetHeight, false)
        return result
    }

    private val clearKeywordsListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val builder = AlertDialog.Builder(this@AddLinkyActivity)
            val message = "키워드를 전부 삭제하시겠습니까?"
            builder.setMessage(message)

            builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                keywords.clear()
                keywordAdapter.notifyDataSetChanged()
            }

            builder.setNegativeButton("취소", null)

            builder.show()
        }
    }

    private val selectPathListener = View.OnClickListener {
        val selectFolderIntent = Intent(this, SelectPathActivity::class.java)
        folderResultLauncher.launch(selectFolderIntent)
    }

    private val imageListener = View.OnClickListener {
        requestPermissions(permissionList, 0)

        // 앨범에서 사진을 선택할 수 있는 액티비티를 실행한다.
        val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // 실행할 액티비티의 타입을 설정한다. (이미지를 선택할 수 있는 타입으로)
        albumIntent.type = "image/*"
        // 선택할 파일의 타입을 지정한다. (안드로이드 OS가 사전작업을 할 수 있도록 하기 위함)
        val mimeType = arrayOf("image/*")
        albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)

        albumResultLauncher.launch(albumIntent)
    }

    private val keywordKeyListener = View.OnKeyListener { v, keyCode, event ->
        var result = false

        if (event?.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    val word: String? = binding.tagTextInput.text.toString()
                    if (word != "") {
                        if (!keywords.contains(word)) {
                            if(keywords.size < 5) {
                                keywords.add(word!!)
                                keywordAdapter.notifyItemInserted(keywords.size - 1)
                                binding.tagTextInput.setText("")
                            } else {
                                binding.tagTextInput.error = "키워드는 최대 5개까지만 가능합니다."
                            }
                        } else {
                            // 이미 존재하는 키워드입니다
                            binding.tagTextInput.error = "이미 존재하는 키워드입니다."
                        }
                    }
                    result = true
                }
            }
        }

        result
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_linky_topbar, menu)

        val item = menu?.findItem(R.id.add_private)
        val relaLayout = item?.actionView as RelativeLayout
        switch = relaLayout.findViewById<Switch>(R.id.add_private)

        val switchListener =
            CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                when(buttonView?.id) {
                    R.id.add_private -> {
                        if(isChecked) {
                            buttonView.text = "공개"
                        } else {
                            buttonView.text = "비공개"
                        }
                    }
                }
            }

        switch.setOnCheckedChangeListener(switchListener)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId) {
            R.id.add_link -> {
                // 키워드가 설정되어 있지 않다는 것을 확인시켜주는 문자메세지 필요

                // 키워드가 있다면
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("from", "add")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}