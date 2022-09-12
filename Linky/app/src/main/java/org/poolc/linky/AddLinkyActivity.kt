package org.poolc.linky

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.util.Patterns
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.poolc.linky.databinding.ActivityAddLinkyBinding
import org.poolc.linky.databinding.DialogChangeProfileImageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern
import kotlin.concurrent.thread

class AddLinkyActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAddLinkyBinding
    private lateinit var app : MyApplication
    private lateinit var purpose : String
    private lateinit var link : Link
    private val keywords = ArrayList<String>()
    private lateinit var keywordAdapter : KeywordAdapter
    private var intentChanged = true
    private lateinit var switch : Switch
    private lateinit var folderResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var albumResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionResultLauncher : ActivityResultLauncher<Intent>
    private val permissionList = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )

    private var mimetype = "image/jpeg"
    private var bitmap : Bitmap? = null
    private var imageFile : File? = null
    private var imageChange = false
    private var fileName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(MyApplication.sharedPref.getString("email", "") == "") {
            val intent = Intent(this, LoginRegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        app = application as MyApplication

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
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    if(result.resultCode == RESULT_OK) {
                        val uri = result.data?.data

                        if(uri != null) {
                            imageChange = true

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // 안드로이드 10 버전부터
                                val source = ImageDecoder.createSource(contentResolver, uri)
                                mimetype = contentResolver.getType(uri).toString()
                                bitmap = ImageDecoder.decodeBitmap(source)
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
                                    bitmap = BitmapFactory.decodeFile(source)
                                    binding.linkImage.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
                else {
                    val builder = AlertDialog.Builder(this)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("이미지 변경 실패")
                    builder.setMessage("'파일 및 미디어' 권한이 허용되어있지 않아 이미지 변경에 실패하였습니다.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
            }

        permissionResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        pickImageFromGallary()
                    }, 200)
                }
            }

        with(binding) {
            // topbar 설정
            setSupportActionBar(addLinkyTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // clear keywords listener 설정
            clearKeywords.setOnClickListener(clearKeywordsListener)

            // image listener 설정
            changePicture.setOnClickListener {
                val builder = AlertDialog.Builder(this@AddLinkyActivity)

                val custom_view = layoutInflater.inflate(R.layout.dialog_change_profile_image, null)

                builder.setView(custom_view)

                val dialog = builder.create()

                val dialogBinding = DialogChangeProfileImageBinding.bind(custom_view)
                dialogBinding.pickAlbum.setOnClickListener {
                    checkImagePermission()

                    dialog.dismiss()
                }

                dialogBinding.pickDefault.setOnClickListener {
                    imageChange = true
                    bitmap = null
                    link.setImgUrl("")
                    linkImage.setImageResource(R.mipmap.linky_logo)

                    dialog.dismiss()
                }

                dialog.show()
            }

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

            tagTextInput.filters = arrayOf(InputFilter { charSequence: CharSequence, i: Int, i1: Int, spanned: Spanned, i2: Int, i3: Int ->
                val ps = Pattern.compile("^[\\s]+$")
                if(ps.matcher(charSequence).matches()) ""
                else charSequence
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

    override fun onDestroy() {
        super.onDestroy()
        if(imageFile != null) {
            imageFile!!.delete()
        }
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

        if(intentChanged) {
            link = Link()
            intentChanged = false
            bitmap = null
            imageFile = null
            imageChange = false

            purpose = intent.getStringExtra("purpose") ?: "add"

            when(purpose) {
                "add" -> {
                    binding.addLinkyTopbarTitle.text = "링크 추가하기"
                    settingForAdd()
                }
                "edit" -> {
                    binding.addLinkyTopbarTitle.text = "링크 수정하기"
                    settingForEdit()
                }
            }
        }
    }

    private fun createTempImageFile(mimetype:String, bitmap:Bitmap) {
        var type = mimetype.substring(mimetype.lastIndexOf("/") + 1, mimetype.length)
        if(type != "jpeg" || type != "png") {
            type = "jpeg"
        }

        fileName = "temp_${System.currentTimeMillis()}.$type"

        imageFile = File.createTempFile(fileName, null, cacheDir)

        try {
            val fos = FileOutputStream(imageFile)

            when(type) {
                "png" -> {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, fos)
                }
                else -> {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos)
                }
            }

            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            imageFile = null
        }
    }

    private fun settingForAdd() {
        with(binding) {
            var url: String? = null
            var imgUrl: String? = null
            var title: String? = null
            var doc: Document? = null
            val pattern = Patterns.WEB_URL

            thread {
                // 공유하기로부터 온 intent 처리
                if (intent.action == Intent.ACTION_SEND) {
                    if (intent.type == "text/plain") {
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

                if (url != null) {
                    var conn: HttpURLConnection? = null
                    try {
                        conn = URL(url).openConnection() as HttpURLConnection
                        if (conn.responseCode in 300..399) {
                            url = conn.getHeaderField("Location")
                            if (url!!.contains("link.naver.com")) {
                                try {
                                    doc = Jsoup.connect(url).userAgent("Chrome").get()
                                    var metaUrl =
                                        doc!!.select("meta[property=al:android:url]").first()
                                            ?.attr("content")
                                    metaUrl = URLDecoder.decode(metaUrl, "UTF-8")
                                    val matcher = pattern.matcher(metaUrl)
                                    if (matcher.find()) {
                                        url = metaUrl!!.substring(
                                            matcher.start(0),
                                            matcher.end(0)
                                        )
                                    }
                                } catch (e: IllegalArgumentException) {
                                    doc = null
                                } catch (e: Exception) {
                                    Log.d("test", e.stackTraceToString())
                                    doc = null
                                }
                            }
                        } else if (url!!.contains("naver.me")) {
                            try {
                                doc = Jsoup.connect(url).userAgent("Chrome").get()
                                var metaUrl = doc!!.select("meta[property=al:android:url]").first()
                                    ?.attr("content")
                                metaUrl = URLDecoder.decode(metaUrl, "UTF-8")
                                val matcher = pattern.matcher(metaUrl)
                                if (matcher.find()) {
                                    url = metaUrl!!.substring(
                                        matcher.start(0),
                                        matcher.end(0)
                                    )
                                }
                                val idx = url!!.indexOf("&version")
                                if (idx != -1) {
                                    url = url!!.substring(
                                        0,
                                        idx
                                    )
                                }
                            } catch (e: IllegalArgumentException) {
                                doc = null
                            } catch (e: Exception) {
                                Log.d("test", e.stackTraceToString())
                                doc = null
                            }
                        }

                        doc = try {
                            url = URLDecoder.decode(url, "UTF-8")
                            Jsoup.connect(url).userAgent("Chrome").get()
                        } catch (e: IllegalArgumentException) {
                            null
                        } catch (e: Exception) {
                            Log.d("test", e.stackTraceToString())
                            null
                        }
                    } catch (e: MalformedURLException) {
                        Log.d("test", "올바르지 않은 URL 주소입니다.")
                    } catch (e: IOException) {
                        Log.d("test", "connection 오류")
                    } finally {
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
                        } catch (e: Exception) {
                            bitmap = null
                            e.printStackTrace()
                        }
                    }

                    runOnUiThread {
                        veil.visibility = View.INVISIBLE

                        // 폴더경로
                        selectPath.visibility = View.VISIBLE

                        // 제목
                        titleTextInput.setText(title ?: "")

                        // 대표이미지
                        if (bitmap != null) {
                            link.setImgUrl(imgUrl ?: "")
                            linkImage.setImageBitmap(bitmap)
                        } else {
                            linkImage.setImageResource(R.mipmap.linky_logo)
                        }

                        // 링크주소
                        link.setUrl(url ?: "")
                        linkAddressTextInput.setText(url)
                        linkAddressTextInput.isEnabled = false
                    }
                }
            }
        }
    }

    private fun settingForEdit() {
        with(binding) {
            thread {
                val path = intent.getStringExtra("path")
                val id = intent.getStringExtra("id")

                val response = app.getLinkInfo(path!!, id!!)

                if(response != "") {
                    val jsonObj = JSONObject(response)
                    val id = jsonObj.getString("id")
                    val title = jsonObj.getString("name")
                    val imgUrl = jsonObj.getString("imageUrl")
                    val url = jsonObj.getString("url")
                    val keywordsJsonArr = jsonObj.getJSONArray("keywords")
                    val isPublic = jsonObj.getString("isPublic").toBoolean()

                    keywords.clear()
                    for(idx in 0 until keywordsJsonArr.length()) {
                        keywords.add(keywordsJsonArr[idx].toString())
                    }

                    var bitmap: Bitmap? = null
                    if (imgUrl != "") {
                        try {
                            val imageUrl: URL? = URL(imgUrl)
                            val conn: HttpURLConnection? =
                                imageUrl?.openConnection() as HttpURLConnection
                            bitmap = BitmapFactory.decodeStream(conn?.inputStream)
                        } catch (e: Exception) {
                            bitmap = null
                            e.printStackTrace()
                        }
                    }

                    link.setId(id)
                    runOnUiThread {
                        switch.isChecked = isPublic

                        keywordAdapter.notifyDataSetChanged()

                        folderPath.text = path
                        selectPath.visibility = View.INVISIBLE

                        titleTextInput.setText(title)

                        if (bitmap != null) {
                            link.setImgUrl(imgUrl ?: "")
                            linkImage.setImageBitmap(bitmap)
                        } else {
                            linkImage.setImageResource(R.mipmap.linky_logo)
                        }

                        link.setUrl(url)
                        linkAddressTextInput.setText(url)
                        linkAddressTextInput.isEnabled = false
                    }
                }
                else {
                    setResult(RESULT_CANCELED)
                    finish()
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

    private fun checkImagePermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
           pickImageFromGallary()
        }
        else if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val builder = AlertDialog.Builder(this)

            builder.setMessage("'파일 및 미디어' 권한이 허용되어야 이미지 변경이 가능합니다.\n" +
                    "아래 설정 버튼을 눌러 '파일 및 미디어' 권한을 허용해주세요.")

            builder.setPositiveButton("설정") { dialogInterface: DialogInterface, i: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:${packageName}"))
                permissionResultLauncher.launch(intent)
            }

            builder.setNegativeButton("취소", null)

            builder.show()
        }
        else {
            requestPermissions(permissionList, 0)
        }
    }

    private fun pickImageFromGallary() {
        val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        albumIntent.type = "image/*"
        val mimeType = arrayOf("image/*")
        albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)

        albumResultLauncher.launch(albumIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for(idx in 0 until grantResults.size) {
            if(grantResults[idx] != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)

                builder.setIcon(R.drawable.ic_baseline_warning_8)
                builder.setTitle("권한 거부됨")
                builder.setMessage("'파일 및 미디어' 권한이 허용되어야 이미지 변경이 가능합니다.")

                builder.setPositiveButton("확인", null)

                builder.show()
                return
            }
        }

        // 모든 권한 허용됨
        pickImageFromGallary()
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
                                "키워드 검색이 불가하더라도 계속하시겠습니까?")

                        when(purpose) {
                            "add" -> {
                                dialog.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                                    createLink()
                                }
                            }
                            "edit" -> {
                                dialog.setPositiveButton("수정") { dialogInterface: DialogInterface, i: Int ->
                                    editLink()
                                }
                            }
                        }

                        dialog.setNegativeButton("취소", null)

                        dialog.show()
                    }
                    else {
                        // 키워드가 있다면
                        when(purpose) {
                            "add" -> {
                                createLink()
                            }
                            "edit" -> {
                                editLink()
                            }
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createLink() {
        val title = binding.titleTextInput.text.toString()

        link.setLinkTitle(title)
        link.setKeywords(keywords)
        link.setIsPublic(switch.isChecked.toString())

        if(bitmap != null) {
            createTempImageFile(mimetype, bitmap!!)
        }

        val email = MyApplication.sharedPref.getString("email", "")
        val path = binding.folderPath.text.toString()
        val body = MultipartBody.Builder()
            .addFormDataPart("email", email)
            .addFormDataPart("isPublic", link.getIsPublic())
            .addFormDataPart("name", link.getLinkTitle())
            .addFormDataPart("path", path)
            .addFormDataPart("url", link.getUrl())
            .addFormDataPart("imageUrl", link.getImgUrl())

        val keywordJsonArr = link.getKeywords()
        if(keywordJsonArr.length() == 0) {
            body.addFormDataPart("keywords", "")
        }
        else {
            for (i in 0 until keywordJsonArr.length()) {
                body.addFormDataPart("keywords[$i]", keywordJsonArr.getString(i))
            }
        }

        if(imageFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile)

            body.addFormDataPart("multipartFile", fileName, requestFile)
        }

        thread {
            val call = MyApplication.service.createLink(body.build())

            call.enqueue(object : Callback<String> {
                override fun onResponse(
                    call: Call<String>,
                    response: Response<String>
                ) {
                    if(imageFile != null) {
                        imageFile!!.delete()
                    }

                    if (response.isSuccessful) {
                        val jsonObj = JSONObject(response.body())
                        val imageSuccess = jsonObj.getString("imageSuccess").toBoolean()
                        if(imageSuccess) {
                            val intent = Intent(this@AddLinkyActivity, MainActivity::class.java)
                            intent.putExtra("from", "add")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finishAndRemoveTask()
                        }
                        else {
                            runOnUiThread {
                                val builder = AlertDialog.Builder(this@AddLinkyActivity)

                                builder.setMessage("이미지 파일을 가져오는데 실패하여 기존 이미지로 추가됩니다.")

                                builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                                    val intent = Intent(this@AddLinkyActivity, MainActivity::class.java)
                                    intent.putExtra("from", "add")
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finishAndRemoveTask()
                                }

                                builder.show()
                            }
                        }
                    } else {
                        var positiveButtonFunc: DialogInterface.OnClickListener? = null
                        var message = ""

                        when (response.code()) {
                            400 -> {
                                message = "형식에 맞지 않는 입력값이 있습니다\n" +
                                        "형식에 알맞게 입력 후, 다시 시도해주세요."
                            }
                            401 -> {
                                message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                                positiveButtonFunc = object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        val editSharedPref = MyApplication.sharedPref.edit()
                                        editSharedPref.remove("email").apply()

                                        val intent = Intent(this@AddLinkyActivity, LoginRegisterActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                        finishAndRemoveTask()
                                    }
                                }
                            }
                            else -> {
                                message = "서버 문제로 링크 추가에 실패하였습니다.\n" +
                                        "잠시후 다시 시도해주세요."
                            }
                        }

                        runOnUiThread {
                            val builder = AlertDialog.Builder(this@AddLinkyActivity)

                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("링크 추가 실패")
                            builder.setMessage(message)

                            builder.setPositiveButton("확인", positiveButtonFunc)

                            builder.show()
                        }
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    if(imageFile != null) {
                        imageFile!!.delete()
                    }

                    runOnUiThread {
                        val builder = AlertDialog.Builder(this@AddLinkyActivity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("링크 추가 실패")
                        builder.setMessage("서버 문제로 링크 추가에 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요.")

                        builder.setPositiveButton("확인", null)

                        builder.show()
                    }
                }
            })
        }
    }

    private fun editLink() {
        val title = binding.titleTextInput.text.toString()

        link.setLinkTitle(title)
        link.setKeywords(keywords)
        link.setIsPublic(switch.isChecked.toString())

        if(bitmap != null) {
            createTempImageFile(mimetype, bitmap!!)
        }

        val email = MyApplication.sharedPref.getString("email", "")
        val path = binding.folderPath.text.toString()
        val body = MultipartBody.Builder()
            .addFormDataPart("email", email)
            .addFormDataPart("id", link.getId())
            .addFormDataPart("imageChange", imageChange.toString())
            .addFormDataPart("isPublic", link.getIsPublic())
            .addFormDataPart("name", link.getLinkTitle())
            .addFormDataPart("path", path)
            .addFormDataPart("url", link.getUrl())

        val keywordJsonArr = link.getKeywords()
        if(keywordJsonArr.length() == 0) {
            body.addFormDataPart("keywords", "")
        }
        else {
            for (i in 0 until keywordJsonArr.length()) {
                body.addFormDataPart("keywords[$i]", keywordJsonArr.getString(i))
            }
        }

        if(imageFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile)

            body.addFormDataPart("multipartFile", fileName, requestFile)
        }

        thread {
            val call = MyApplication.service.editLink(body.build())

            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if(imageFile != null) {
                        imageFile!!.delete()
                    }

                    if (response.isSuccessful) {
                        val jsonObj = JSONObject(response.body())
                        val imageSuccess = jsonObj.getString("imageSuccess").toBoolean()
                        if(imageSuccess) {
                            editCompleted(response.code())
                        }
                        else {
                            runOnUiThread {
                                val builder = AlertDialog.Builder(this@AddLinkyActivity)

                                builder.setMessage("이미지 파일을 가져오는데 실패하여 기존 이미지로 수정됩니다.")

                                builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                                    editCompleted(response.code())
                                }

                                builder.show()
                            }
                        }
                    } else {
                        editCompleted(response.code())
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    if(imageFile != null) {
                        imageFile!!.delete()
                    }

                    runOnUiThread {
                        val builder = AlertDialog.Builder(this@AddLinkyActivity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("링크 수정 실패")
                        builder.setMessage("서버 문제로 링크 수정에 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요.")

                        builder.setPositiveButton("확인", null)

                        builder.show()
                    }
                }
            })
        }
    }

    private fun editCompleted(responseCode: Int) {
        intent.putExtra("responseCode", responseCode)
        setResult(RESULT_OK, intent)
        finishAndRemoveTask()
    }

    private fun showErrorMessage(message:String) {
        var title = ""
        when(purpose) {
            "add" -> {
                title = "링크 추가 실패"
            }
            "edit" -> {
                title = "링크 수정 실패"
            }
        }

        val dialog = AlertDialog.Builder(this)
        dialog.setIcon(R.drawable.ic_baseline_warning_8)
        dialog.setTitle(title)
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
            else if(titleTextInput.text.toString().trim() == "") {
                showErrorMessage("앞/뒤 공백 없이 최소 1자 이상의 제목을 입력해주세요.")
            }
            else if(titleTextInput.text!!.length > 50) {
                showErrorMessage("제목이 너무 깁니다.\n" +
                        "50자 이하로 작성해주세요.")
            }
            else {
                result = true
            }
        }

        return result
    }
}