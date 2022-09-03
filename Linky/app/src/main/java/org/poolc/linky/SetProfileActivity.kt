package org.poolc.linky

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import org.poolc.linky.databinding.ActivitySetProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import kotlin.concurrent.thread

class SetProfileActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySetProfileBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        albumResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if(result.resultCode == RESULT_OK) {
                    val uri = result.data?.data

                    if(uri != null) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // 안드로이드 10 버전부터
                            val source = ImageDecoder.createSource(contentResolver, uri)
                            mimetype = contentResolver.getType(uri).toString()
                            bitmap = ImageDecoder.decodeBitmap(source)
                            binding.firstProfileImage.setImageBitmap(bitmap)
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
                                binding.firstProfileImage.setImageBitmap(bitmap)
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
            profile.setOnClickListener(imageListener)

            register.setOnClickListener {
                val nicknameTrim = nicknameTextInput.text.toString().trim()
                val pattern = Pattern.compile("^[ㄱ-ㅣ가-힣a-zA-Z\\s]+$")

                if(nicknameTrim == "") {
                    val builder = AlertDialog.Builder(this@SetProfileActivity)
                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("회원가입 실패")
                    builder.setMessage("닉네임은 앞/뒤 공백 없이 1자 이상 입력해주세요.")
                    builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                        binding.nicknameTextInput.setText("")
                    }
                    builder.show()
                }
                else if(!pattern.matcher(nicknameTrim).matches()) {
                    val builder = AlertDialog.Builder(this@SetProfileActivity)
                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("회원가입 실패")
                    builder.setMessage("닉네임은 한글, 영어, 사이 공백만 가능합니다.")
                    builder.setPositiveButton("확인", null)
                    builder.show()
                }
                else {
                    if (bitmap != null) {
                        createTempImageFile(mimetype, bitmap!!)
                    }

                    register(nicknameTrim)
                }
            }
        }
    }

    private fun register(nickname:String) {
        val email = intent.getStringExtra("email")!!
        val emailBody = RequestBody.create(MediaType.parse("text/plain"), email)
        val password = intent.getStringExtra("password")!!
        val passwordBody = RequestBody.create(MediaType.parse("text/plain"), password)
        val nicknameBody = RequestBody.create(MediaType.parse("text/plain"), nickname)
        var multipartBody: MultipartBody.Part? = null

        if(imageFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile)

            multipartBody = MultipartBody.Part.createFormData(
                "multipartFile",
                imageFile!!.name,
                requestFile
            )
        }

        val body = HashMap<String, RequestBody>()
        body["email"] = emailBody
        body["password"] = passwordBody
        body["nickname"] = nicknameBody

        thread {
            val call = MyApplication.service.register(body, multipartBody)

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
                            val intent = Intent(this@SetProfileActivity, LoginRegisterActivity::class.java)
                            intent.putExtra("result", "success register")
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                        else {
                            runOnUiThread {
                                val builder = AlertDialog.Builder(this@SetProfileActivity)

                                builder.setMessage("이미지 파일을 가져오는데 실패하여 기존 이미지로 저장됩니다.")

                                builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                                    val intent = Intent(this@SetProfileActivity, LoginRegisterActivity::class.java)
                                    intent.putExtra("result", "success register")
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                }

                                builder.show()
                            }
                        }
                    } else {
                        var positiveButtonFunc: DialogInterface.OnClickListener? = null
                        var message = ""

                        when (response.code()) {
                            400 -> {
                                message = "이메일 형식 또는 비밀번호 형식이 잘못되었습니다."
                            }
                            409 -> {
                                message = "이미 존재하는 닉네임 입니다."
                            }
                            else -> {
                                message = "서버 문제로 프로필 수정에 실패하였습니다.\n" +
                                        "잠시후 다시 시도해주세요."
                            }
                        }

                        runOnUiThread {
                            val builder = AlertDialog.Builder(this@SetProfileActivity)

                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("회원가입 실패")
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
                        val builder = AlertDialog.Builder(this@SetProfileActivity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("회원가입 실패")
                        builder.setMessage("서버 문제로 회원가입에 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요.")

                        builder.setPositiveButton("확인", null)

                        builder.show()
                    }
                }
            })
        }
    }

    private fun createTempImageFile(mimetype:String, bitmap:Bitmap) {
        var type = mimetype.substring(mimetype.lastIndexOf("/") + 1, mimetype.length)
        if(type != "jpeg" || type != "png") {
            type = "jpeg"
        }

        val file_name = "temp_${System.currentTimeMillis()}.$type"

        imageFile = File.createTempFile(file_name, null, cacheDir)

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

    private fun pickImageFromGallary() {
        val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        albumIntent.type = "image/*"
        val mimeType = arrayOf("image/*")
        albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)

        albumResultLauncher.launch(albumIntent)
    }

    private val imageListener = View.OnClickListener {
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
}