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
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.poolc.linky.databinding.ActivityAddLinkyBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
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

    private var imgUrl : String? = null
    private var url : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(MyApplication.sharedPref.getString("userEmail", "") == "") {
            val intent = Intent(this, LoginRegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

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
                        binding.linkImage.setImageBitmap(bitmap)
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
                            binding.linkImage.setImageBitmap(bitmap)
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

            // keyword textinput done IME 이벤트 설정
            tagTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_DONE) {
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
                    true
                }
                false
            }

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

    override fun onBackPressed() {
        super.onBackPressed()
        finishAndRemoveTask()
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
                imgUrl = null
                url = null
                var title : String? = null
                var doc : Document? = null
                val pattern = Patterns.WEB_URL

                thread {
                    // 공유하기로부터 온 intent 처리
                    if (intent.action == Intent.ACTION_SEND) {
                        if(intent.type == "text/plain") {
                            runOnUiThread {
                                veil.visibility = View.VISIBLE
                            }
                            val txt = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
                            val txtDecoded = URLDecoder.decode(txt, "UTF-8")

                            val matcher = pattern.matcher(txtDecoded)
                            if (matcher.find()) {
                                url = txtDecoded.substring(matcher.start(0), matcher.end(0))
                            }
                        }
                    } else {
                        runOnUiThread {
                            veil.visibility = View.VISIBLE
                        }
                        url = intent.getStringExtra("url")
                    }

                    if(url != null) {
                        var conn : HttpURLConnection? = null
                        try {
                            conn = URL(url).openConnection() as HttpURLConnection
                            if (conn.responseCode in 300..399) {
                                url = conn.getHeaderField("Location")
                                if(url!!.contains("link.naver.com")) {
                                    try {
                                        doc = Jsoup.connect(url).userAgent("Chrome").get()
                                        var metaUrl = doc!!.select("meta[property=al:android:url]").first()
                                            ?.attr("content")
                                        metaUrl = URLDecoder.decode(metaUrl, "UTF-8")
                                        val matcher = pattern.matcher(metaUrl)
                                        if(matcher.find()) {
                                            url = metaUrl!!.substring(
                                                matcher.start(0),
                                                matcher.end(0)
                                            )
                                        }
                                    } catch (e: IllegalArgumentException) {
                                        doc = null
                                    } catch (e:Exception) {
                                        Log.d("test", e.stackTraceToString())
                                        doc = null
                                    }
                                }
                            }
                            else if(url!!.contains("naver.me")) {
                                try {
                                    doc = Jsoup.connect(url).userAgent("Chrome").get()
                                    var metaUrl = doc!!.select("meta[property=al:android:url]").first()
                                        ?.attr("content")
                                    metaUrl = URLDecoder.decode(metaUrl, "UTF-8")
                                    val matcher = pattern.matcher(metaUrl)
                                    if(matcher.find()) {
                                        url = metaUrl!!.substring(
                                            matcher.start(0),
                                            matcher.end(0)
                                        )
                                    }
                                    val idx = url!!.indexOf("&version")
                                    if(idx != -1) {
                                        url = url!!.substring(
                                            0,
                                            idx
                                        )
                                    }
                                } catch (e: IllegalArgumentException) {
                                    doc = null
                                } catch (e:Exception) {
                                    Log.d("test", e.stackTraceToString())
                                    doc = null
                                }
                            }

                            doc = try {
                                url = URLDecoder.decode(url, "UTF-8")
                                Jsoup.connect(url).userAgent("Chrome").get()
                            } catch (e: IllegalArgumentException) {
                                null
                            } catch (e:Exception) {
                                Log.d("test", e.stackTraceToString())
                                null
                            }
                        } catch (e: MalformedURLException) {
                            Log.d("test", "올바르지 않은 URL 주소입니다.")
                        } catch (e: IOException) {
                            Log.d("test", "connection 오류")
                        }finally {
                            conn?.disconnect()
                        }
                    }

                    if (doc == null) {
                        runOnUiThread {
                            veil.visibility = View.INVISIBLE
                            val builder = AlertDialog.Builder(this@AddLinkyActivity)
                            val title = "URL 오류"
                            val message = "존재하지 않는 url 입니다."
                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle(title)
                            builder.setMessage(message)

                            builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                                val intent = Intent(this@AddLinkyActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finishAndRemoveTask()
                            }

                            builder.show()
                        }
                    } else {
                        var metaTags = doc!!.select("meta[property]")
                        for (meta in metaTags) {
                            if (title != null && imgUrl != null) {
                                break
                            }

                            if (title == null && meta.attr("property").contains("title")) {
                                title = meta.attr("content")
                            }

                            if (imgUrl == null && meta.attr("property").contains("image")) {
                                imgUrl = meta.attr("content")
                            }
                        }

                        metaTags = doc!!.select("meta[name]")
                        for (meta in metaTags) {
                            if (title != null && imgUrl != null) {
                                break
                            }

                            if (title == null && meta.attr("name").contains("title")) {
                                title = meta.attr("content")
                            }

                            if (imgUrl == null && meta.attr("name").contains("image")) {
                                imgUrl = meta.attr("content")
                            }
                        }

                        // TODO 키워드 제한으로 일단 삭제
                        /*val keywordsStr : String? =
                        doc.select("meta[name=keywords]").first()?.attr("content")
                        if(keywordsStr != null) {
                            val keywordsArr = keywordsStr.split(",")
                            keywords.addAll(keywordsArr)
                        }*/

                        if (title == null) {
                            title = doc!!.getElementsByTag("title")?.first()?.text()
                        }

                        if (imgUrl == null) {
                            imgUrl = doc!!.getElementsByTag("img").first()?.absUrl("src")
                        }

                        var bitmap: Bitmap? = null
                        if (imgUrl != null) {
                            try {
                                if (!imgUrl!!.contains("http")) {
                                    imgUrl = "http://$imgUrl"
                                }
                                val imageUrl: URL? = URL(imgUrl)
                                val conn: HttpURLConnection? =
                                    imageUrl?.openConnection() as HttpURLConnection
                                bitmap = BitmapFactory.decodeStream(conn?.inputStream)
                            } catch (e: MalformedURLException) {
                                bitmap = null
                                Log.d("test", "MalformedURLException")
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
                            if (bitmap != null) {
                                linkImage.setImageBitmap(bitmap)
                            } else {
                                linkImage.setImageResource(R.mipmap.linky_logo)
                            }

                            // 링크주소
                            linkAddressTextInput.setText(url)
                            linkAddressTextInput.isEnabled = false
                        }
                    }
                }
            }
        }
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
                if(verify()) {
                    // 키워드가 설정되어 있지 않다는 것을 확인시켜주는 문자메세지 필요
                    if(keywords.size == 0) {
                        val dialog = AlertDialog.Builder(this)
                        dialog.setIcon(R.drawable.no_keyword)
                        dialog.setTitle("키워드 없음")
                        dialog.setMessage("설정된 키워드가 없습니다.\n" +
                                "키워드 검색이 불가하더라도 추가하시겠습니까?")

                        dialog.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                            createLink()
                        }

                        dialog.setNegativeButton("취소", null)

                        dialog.show()
                    }
                    else {
                        // 키워드가 있다면
                        createLink()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createLink() {
        val app = application as MyApplication

        val path = binding.folderPath.text.toString()
        val title = binding.titleTextInput.text.toString()

        // TODO 이미지 URL 인지 BITMAP 인지 확인

        thread {
            val responseCode = app.createLink(switch.isChecked, keywords, path, title, imgUrl!!, url!!)

            when (responseCode) {
                200 -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("from", "add")
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                400 -> {
                    Log.d("test", "Bad request")
                }
                404 -> {
                    Log.d("test", "Not Found")
                }
                401 -> {
                    Log.d("test", "Unauthorized")
                }
            }
        }
    }

    private fun showErrorMessage(message:String) {
        val dialog = AlertDialog.Builder(this)
        dialog.setIcon(R.drawable.ic_baseline_warning_8)
        dialog.setTitle("링크 추가 실패")
        dialog.setMessage(message)

        dialog.setPositiveButton("확인", null)

        dialog.show()
    }

    private fun verify():Boolean {
        var result = false
        with(binding) {
            if (folderPath.text == getString(R.string.default_path)) {
                showErrorMessage("저장할 위치를 지정해주세요.")
            }
            else if(titleTextInput.text.toString() == "") {
                showErrorMessage("제목을 입력해주세요.")
            }
            else if(titleTextInput.text!!.length > 20) {
                showErrorMessage("제목이 너무 깁니다.\n" +
                        "20자 이하로 작성해주세요.")
            }
            else {
                result = true
            }
        }

        return result
    }
}