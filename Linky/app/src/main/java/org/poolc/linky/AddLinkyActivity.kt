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
import com.google.gson.JsonElement
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

        if(MyApplication.sharedPref.getString("token", "") == "") {
            val title = "????????? ??????"
            val message = "???????????? ????????? ????????? ?????????.\n" +
                    "????????? ??? ?????? ??????????????????."
            val listener = DialogInterface.OnDismissListener {
                val intent =
                    Intent(this@AddLinkyActivity, LoginRegisterActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            showDialog(title, message, listener)
        }
        else {
            val call = MyApplication.service.verifyToken()
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    val title = "?????? ??????"
                    val message = "???????????? ?????? ????????? ?????? ?????? ????????? ???????????????.\n" +
                            "????????? ?????? ??????????????????."
                    val listener = DialogInterface.OnDismissListener { finish() }

                    showDialog(title, message, listener)
                }
            })
        }

        app = application as MyApplication

        keywordAdapter = KeywordAdapter(keywords, object : KeywordAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val builder = AlertDialog.Builder(this@AddLinkyActivity)
                val message = "????????? '${keywords[pos]}' ???/??? ?????????????????????????"
                builder.setMessage(message)

                builder.setPositiveButton("??????") { dialogInterface: DialogInterface, i: Int ->
                    keywords.removeAt(pos)
                    keywordAdapter.notifyItemRemoved(pos)
                    binding.tagTextInput.error = null
                }

                builder.setNegativeButton("??????", null)

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
                                // ??????????????? 10 ????????????
                                val source = ImageDecoder.createSource(contentResolver, uri)
                                mimetype = contentResolver.getType(uri).toString()
                                bitmap = ImageDecoder.decodeBitmap(source)
                                binding.linkImage.setImageBitmap(bitmap)
                            }
                            else {
                                // ??????????????? 9 ????????????
                                val cursor = contentResolver.query(uri, null, null, null, null)
                                if(cursor != null) {
                                    cursor.moveToNext()
                                    // ????????? ????????? ????????????.
                                    val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                                    val source = cursor.getString(index)
                                    // ???????????? ????????????.
                                    bitmap = BitmapFactory.decodeFile(source)
                                    binding.linkImage.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
                else {
                    val title = "????????? ?????? ??????"
                    val message = "'?????? ??? ?????????' ????????? ?????????????????? ?????? ????????? ????????? ?????????????????????."
                    showDialog(title, message, null)
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
            // topbar ??????
            setSupportActionBar(addLinkyTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // clear keywords listener ??????
            clearKeywords.setOnClickListener(clearKeywordsListener)

            // image listener ??????
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

            // keyword textinput done IME ????????? ??????
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
                                binding.tagTextInput.error = "???????????? ?????? 5???????????? ???????????????."
                            }
                        } else {
                            // ?????? ???????????? ??????????????????
                            binding.tagTextInput.error = "?????? ???????????? ??????????????????."
                        }
                    }
                    true
                }
                false
            }

            // keyword recyclerview adapter ??????
            keywordRecycler.adapter = keywordAdapter

            // keyword recyclerview item ?????? ??????
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

            // select path listener ??????
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
                // ??? ?????????
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
                    binding.addLinkyTopbarTitle.text = "?????? ????????????"
                    settingForAdd()
                }
                "edit" -> {
                    binding.addLinkyTopbarTitle.text = "?????? ????????????"
                    settingForEdit()
                }
            }
        }
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("??????", null)

        builder.show()
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
                // ????????????????????? ??? intent ??????
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
                    url = intent.getStringExtra("url") ?: ""
                    if (!url!!.contains("http")) {
                        url = "http://$url"
                    }
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
                            Jsoup.connect(url).userAgent("Chrome").get()
                        } catch (e: IllegalArgumentException) {
                            null
                        } catch (e: Exception) {
                            Log.d("test", e.stackTraceToString())
                            null
                        }
                    } catch (e: MalformedURLException) {
                        Log.d("test", "???????????? ?????? URL ???????????????.")
                    } catch (e: IOException) {
                        Log.d("test", "connection ??????")
                    } finally {
                        conn?.disconnect()
                    }
                }

                if (doc == null) {
                    runOnUiThread {
                        veil.visibility = View.INVISIBLE
                        val builder = AlertDialog.Builder(this@AddLinkyActivity)
                        val title = "URL ??????"
                        val message = "???????????? ?????? url ?????????."
                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle(title)
                        builder.setMessage(message)

                        builder.setPositiveButton("??????") { dialogInterface: DialogInterface, i: Int ->
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

                        // ????????????
                        selectPath.visibility = View.VISIBLE

                        // ??????
                        titleTextInput.setText(title ?: "")

                        // ???????????????
                        if (bitmap != null) {
                            link.setImgUrl(imgUrl ?: "")
                            linkImage.setImageBitmap(bitmap)
                        } else {
                            linkImage.setImageResource(R.mipmap.linky_logo)
                        }

                        // ????????????
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
            val path = intent.getStringExtra("path") ?: ""
            val id = intent.getStringExtra("id") ?: ""

            val call = MyApplication.service.getLinkInfo(path, id)

            call.enqueue(object: Callback<JsonElement> {
                override fun onResponse(
                    call: Call<JsonElement>,
                    response: Response<JsonElement>
                ) {
                    if(response.isSuccessful) {
                        val jsonObj = response.body()!!.asJsonObject
                        if(!jsonObj.isJsonNull) {
                            val id = jsonObj.get("id").asString
                            val title = jsonObj.get("name").asString
                            val imgUrl = jsonObj.get("imageUrl").asString
                            val url = jsonObj.get("url").asString
                            val keywordsArr = jsonObj.getAsJsonArray("keywords")
                            val keywordsJsonArr = JSONArray()
                            for(keyword in keywordsArr) {
                                keywordsJsonArr.put(keyword.asString)
                            }
                            val isPublic = jsonObj.get("isPublic").asBoolean

                            keywords.clear()
                            for(idx in 0 until keywordsJsonArr.length()) {
                                keywords.add(keywordsJsonArr[idx].toString())
                            }

                            var image: Bitmap? = null
                            if (imgUrl != "") {
                                thread {
                                    image = app.getImageUrl(imgUrl)

                                    if (image != null) {
                                        runOnUiThread {
                                            link.setImgUrl(imgUrl ?: "")
                                            linkImage.setImageBitmap(image)
                                        }
                                    } else {
                                        runOnUiThread {
                                            linkImage.setImageResource(R.mipmap.linky_logo)
                                        }
                                    }
                                }
                            }
                            else {
                                linkImage.setImageResource(R.mipmap.linky_logo)
                            }

                            link.setId(id)
                            switch.isChecked = isPublic

                            keywordAdapter.notifyDataSetChanged()

                            folderPath.text = path
                            selectPath.visibility = View.INVISIBLE

                            titleTextInput.setText(title)

                            link.setUrl(url)
                            linkAddressTextInput.setText(url)
                            linkAddressTextInput.isEnabled = false
                        }
                        else {
                            val title = "?????? ?????? ???????????? ??????"
                            val message = "?????? ????????? ????????? ??????????????? ?????????????????????.\n" +
                                    "????????? ?????? ??????????????????."
                            val listener = DialogInterface.OnDismissListener {
                                setResult(RESULT_CANCELED)
                                finish()
                            }

                            showDialog(title, message, listener)
                        }
                    }
                    else {
                        val title = "?????? ?????? ???????????? ??????"
                        var message = "?????? ????????? ?????? ????????? ??????????????? ?????????????????????."
                        var listener = DialogInterface.OnDismissListener {
                            setResult(RESULT_CANCELED)
                            finish()
                        }

                        when(response.code()) {
                            404 -> {
                                message = "???????????? ?????? ???????????????."
                            }
                        }

                        showDialog(title, message, listener)
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    val title = "?????? ?????? ???????????? ??????"
                    val message = "???????????? ?????? ????????? ????????? ??????????????? ?????????????????????.\n" +
                            "????????? ?????? ??????????????????."
                    val listener = DialogInterface.OnDismissListener {
                        setResult(RESULT_CANCELED)
                        finish()
                    }

                    showDialog(title, message, listener)
                }
            })
        }
    }

    private val clearKeywordsListener = View.OnClickListener {
        val builder = AlertDialog.Builder(this@AddLinkyActivity)
        val message = "???????????? ?????? ?????????????????????????"
        builder.setMessage(message)

        builder.setPositiveButton("??????") { dialogInterface: DialogInterface, i: Int ->
            keywords.clear()
            keywordAdapter.notifyDataSetChanged()
        }

        builder.setNegativeButton("??????", null)

        builder.show()
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

            builder.setMessage("'?????? ??? ?????????' ????????? ??????????????? ????????? ????????? ???????????????.\n" +
                    "?????? ?????? ????????? ?????? '?????? ??? ?????????' ????????? ??????????????????.")

            builder.setPositiveButton("??????") { dialogInterface: DialogInterface, i: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:${packageName}"))
                permissionResultLauncher.launch(intent)
            }

            builder.setNegativeButton("??????", null)

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
                val title = "?????? ?????????"
                val message = "'?????? ??? ?????????' ????????? ??????????????? ????????? ????????? ???????????????."
                showDialog(title, message, null)
                return
            }
        }

        // ?????? ?????? ?????????
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
                            buttonView.text = "??????"
                        } else {
                            buttonView.text = "?????????"
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
                    // ???????????? ???????????? ?????? ????????? ?????? ?????????????????? ??????????????? ??????
                    if(keywords.size == 0) {
                        val dialog = AlertDialog.Builder(this)
                        dialog.setIcon(R.drawable.no_keyword)
                        dialog.setTitle("????????? ??????")
                        dialog.setMessage("????????? ???????????? ????????????.\n" +
                                "????????? ????????? ?????????????????? ?????????????????????????")

                        when(purpose) {
                            "add" -> {
                                dialog.setPositiveButton("??????") { dialogInterface: DialogInterface, i: Int ->
                                    createLink()
                                }
                            }
                            "edit" -> {
                                dialog.setPositiveButton("??????") { dialogInterface: DialogInterface, i: Int ->
                                    editLink()
                                }
                            }
                        }

                        dialog.setNegativeButton("??????", null)

                        dialog.show()
                    }
                    else {
                        // ???????????? ?????????
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

        val path = binding.folderPath.text.toString()
        val body = MultipartBody.Builder()
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
                        val title = "????????? ?????? ???????????? ??????"
                        val message = "????????? ????????? ??????????????? ???????????? ?????? ???????????? ???????????????."
                        val listener = DialogInterface.OnDismissListener {
                            val intent = Intent(this@AddLinkyActivity, MainActivity::class.java)
                            intent.putExtra("from", "add")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finishAndRemoveTask()
                        }
                        showDialog(title, message, listener)
                    }
                } else {
                    val title = "?????? ?????? ??????"
                    var message = "?????? ????????? ?????? ?????? ????????? ?????????????????????."
                    var listener: DialogInterface.OnDismissListener? = null

                    when (response.code()) {
                        400 -> {
                            message = "????????? ?????? ?????? ???????????? ????????????\n" +
                                    "????????? ????????? ?????? ???, ?????? ??????????????????."
                        }
                        404 -> {
                            message = "?????? ????????? ???????????? ????????????.\n" +
                                    "????????? ?????? ?????? ???, ?????? ??????????????????."
                            listener = DialogInterface.OnDismissListener {
                                binding.folderPath.text = getString(R.string.default_path)
                            }
                        }
                    }

                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                if(imageFile != null) {
                    imageFile!!.delete()
                }

                val title = "?????? ?????? ??????"
                val message = "???????????? ?????? ????????? ?????? ????????? ?????????????????????.\n" +
                        "????????? ?????? ??????????????????."
                showDialog(title, message, null)
            }
        })
    }

    private fun editLink() {
        val title = binding.titleTextInput.text.toString()

        link.setLinkTitle(title)
        link.setKeywords(keywords)
        link.setIsPublic(switch.isChecked.toString())

        if(bitmap != null) {
            createTempImageFile(mimetype, bitmap!!)
        }

        val path = binding.folderPath.text.toString()
        val body = MultipartBody.Builder()
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
                        val title = "????????? ?????? ???????????? ??????"
                        val message = "????????? ????????? ??????????????? ???????????? ?????? ???????????? ???????????????."
                        val listener = DialogInterface.OnDismissListener {
                            editCompleted(response.code())
                        }
                        showDialog(title, message, listener)
                    }
                } else {
                    editCompleted(response.code())
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                if(imageFile != null) {
                    imageFile!!.delete()
                }

                val title = "?????? ?????? ??????"
                val message =  "???????????? ?????? ????????? ?????? ????????? ?????????????????????.\n" +
                        "????????? ?????? ??????????????????."
                showDialog(title, message, null)
            }
        })
    }

    private fun editCompleted(responseCode: Int) {
        intent.putExtra("responseCode", responseCode)
        setResult(RESULT_OK, intent)
        finishAndRemoveTask()
    }

    private fun verify():Boolean {
        var result = false
        var title = ""
        when(purpose) {
            "add" -> {
                title = "?????? ?????? ??????"
            }
            "edit" -> {
                title = "?????? ?????? ??????"
            }
        }
        var message = ""

        with(binding) {
            if (folderPath.text == getString(R.string.default_path)) {
                message = "????????? ????????? ??????????????????."
                showDialog(title, message, null)
            }
            else if(titleTextInput.text.toString().trim() == "") {
                message = "???/??? ?????? ?????? ?????? 1??? ????????? ????????? ??????????????????."
                showDialog(title, message, null)
            }
            else if(titleTextInput.text!!.length > 50) {
                message = "????????? ?????? ?????????.\n" +
                        "50??? ????????? ??????????????????."
                showDialog(title, message, null)
            }
            else {
                result = true
            }
        }

        return result
    }
}