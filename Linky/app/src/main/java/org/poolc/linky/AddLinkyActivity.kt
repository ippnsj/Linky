package org.poolc.linky

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import org.jsoup.Jsoup
import org.poolc.linky.databinding.ActivityAddLinkyBinding
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class AddLinkyActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAddLinkyBinding
    private var folders = arrayOf("선택안함", "음식", "고양이", "직접입력")
    private val keywords = ArrayList<String>()
    private lateinit var keywordAdapter : KeywordAdapter
    private lateinit var albumResultLauncher : ActivityResultLauncher<Intent>
    private val permission_list = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keywordAdapter = KeywordAdapter(keywords)

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

            // spinner 설정
            val spinnerAdapter = ArrayAdapter(this@AddLinkyActivity, android.R.layout.simple_spinner_item, folders)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            folderSpinner.adapter = spinnerAdapter
            folderSpinner.onItemSelectedListener = spinnerListener
            folderTextInput.isEnabled = false

            // image listener 설정
            linkImage.setOnClickListener(imageListener)

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

            // 공유하기로부터 온 intent 처리
            if(intent.action == Intent.ACTION_SEND && intent.type != null) {
                if(intent.type == "text/plain") {
                    veil.visibility = View.VISIBLE
                    val txt = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
                    val pattern = Pattern.compile("((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(txt)
                    if(matcher.find()) {
                        val url = txt.substring(matcher.start(0), matcher.end(0))

                        // 메타데이터 추출
                        thread {
                            val doc = Jsoup.connect(url).get()
                            val title =
                                doc.select("meta[property=og:title]").first()?.attr("content");
                            val image =
                                doc.select("meta[property=og:image]").get(0).attr("content")

                            val imageUrl = URL(image)
                            val conn = imageUrl.openConnection() as HttpURLConnection
                            val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                            var resizedBitmap:Bitmap? = null
                            if(bitmap != null) {
                                // resizedBitmap = resizeBitmap(1024, bitmap)
                                resizedBitmap = resizeBitmap(resources.displayMetrics.widthPixels - 100, bitmap)
                            }

                            runOnUiThread {
                                veil.visibility = View.INVISIBLE

                                // 링크주소
                                linkAddressTextInput.setText(url)
                                linkAddressTextInput.isEnabled = false

                                // 제목
                                titleTextInput.setText(title ?: "")

                                // 대표이미지
                                if(resizedBitmap != null) {
                                    linkImage.setImageBitmap(resizedBitmap)
                                }
                            }
                        }
                    }
                    else {
                        Log.d("test", "NO URL")
                    }
                }
            }
        }
    }

    // 사진의 사이즈를 조정하는 메서드
    fun resizeBitmap(targetWidth:Int, img: Bitmap) : Bitmap {
        // 이미지의 비율을 계산한다.
        val ratio = targetWidth.toDouble() / img.width.toDouble()
        // 보정될 세로 길이를 구한다.
        val targetHeight = (img.height * ratio).toInt()
        // 크기를 조정한 bitmap 객체를 생성한다.
        val result = Bitmap.createScaledBitmap(img, targetWidth, targetHeight, false)
        return result
    }

    val spinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            when(parent?.id) {
                R.id.folderSpinner -> {
                    if(position == folders.size - 1) {
                        binding.folderTextInput.setText("")
                        binding.folderTextInput.isEnabled = true
                    }
                    else {
                        binding.folderTextInput.setText(folders[position])
                        binding.folderTextInput.isEnabled = false
                    }
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    val imageListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            requestPermissions(permission_list, 0)

            // 앨범에서 사진을 선택할 수 있는 액티비티를 실행한다.
            val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            // 실행할 액티비티의 타입을 설정한다. (이미지를 선택할 수 있는 타입으로)
            albumIntent.type = "image/*"
            // 선택할 파일의 타입을 지정한다. (안드로이드 OS가 사전작업을 할 수 있도록 하기 위함)
            val mimeType = arrayOf("image/*")
            albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)

            albumResultLauncher.launch(albumIntent)
        }
    }

    val keywordKeyListener = object : View.OnKeyListener {
        override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
            var result = false

            if (event?.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> {
                        val word: String? = binding.tagTextInput.text.toString()
                        if (word != "") {
                            if (!keywords.contains(word)) {
                                keywords.add(word!!)
                                keywordAdapter.notifyItemInserted(keywords.size - 1)
                                binding.tagTextInput.setText("")
                            } else {
                                // 이미 존재하는 키워드입니다
                                Log.d("test exist", "이미 존재하는 키워드 입니다.")
                            }
                        }
                        result = true
                    }
                }
            }

            return result
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_linky_topbar, menu)

        val item = menu?.findItem(R.id.add_private)
        val relaLayout = item?.actionView as RelativeLayout
        val switch = relaLayout.findViewById<Switch>(R.id.add_private)

        val switchListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                when(buttonView?.id) {
                    R.id.add_private -> {
                        if(isChecked) {
                            buttonView.setText("공개")
                        }
                        else {
                            buttonView.setText("비공개")
                        }
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
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}