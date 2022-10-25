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
import org.poolc.linky.databinding.DialogChangeProfileImageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

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
                val title = "이미지 변경 실패"
                val message = "'파일 및 미디어' 권한이 허용되어있지 않아 이미지 변경에 실패하였습니다."
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
            profile.setOnClickListener {
                val builder = AlertDialog.Builder(this@SetProfileActivity)

                val custom_view = layoutInflater.inflate(R.layout.dialog_change_profile_image, null)

                builder.setView(custom_view)

                val dialog = builder.create()

                val dialogBinding = DialogChangeProfileImageBinding.bind(custom_view)
                dialogBinding.pickAlbum.setOnClickListener {
                    checkImagePermission()

                    dialog.dismiss()
                }

                dialogBinding.pickDefault.setOnClickListener {
                    bitmap = null
                    firstProfileImage.setImageResource(R.drawable.profile)

                    dialog.dismiss()
                }

                dialog.show()
            }

            register.setOnClickListener {
                val nicknameTrim = nicknameTextInput.text.toString().trim()
                val pattern = Pattern.compile("^[ㄱ-ㅣ가-힣a-zA-Z\\s]+$")
                val title = "회원가입 실패"

                if(nicknameTrim == "") {
                    val message = "닉네임은 앞/뒤 공백 없이 1자 이상 입력해주세요."
                    val listener = DialogInterface.OnDismissListener {
                        binding.nicknameTextInput.setText("")
                    }
                    showDialog(title, message, listener)
                }
                else if(!pattern.matcher(nicknameTrim).matches()) {
                    val message = "닉네임은 한글, 영어, 사이 공백만 가능합니다."
                    showDialog(title, message, null)
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

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this@SetProfileActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    private fun register(nickname:String) {
        val email = intent.getStringExtra("email") ?: ""
        val password = intent.getStringExtra("password") ?: ""

        val body = MultipartBody.Builder()
            .addFormDataPart("email", email)
            .addFormDataPart("password", password)
            .addFormDataPart("nickname", nickname)

        if(imageFile != null) {
            val requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile)

            body.addFormDataPart("multipartFile", imageFile!!.name, requestFile)
        }

        val call = MyApplication.service.register(body.build())

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
                        showEmailDialog()
                    }
                    else {
                        val title = "이미지 파일 가져오기 실패"
                        val message = "이미지 파일을 가져오는데 실패하여 기존 이미지로 생성됩니다."
                        val listener = DialogInterface.OnDismissListener {
                            showEmailDialog()
                        }
                        showDialog(title, message, listener)
                    }
                } else {
                    val title = "회원가입 실패"
                    var message = "서버 문제로 인해 회원가입에 실패하였습니다."

                    when (response.code()) {
                        400 -> {
                            message = "이메일 형식 또는 비밀번호 형식이 잘못되었습니다."
                        }
                        409 -> {
                            message = "이미 존재하는 이메일 또는 닉네임 입니다."
                        }
                    }

                    showDialog(title, message, null)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                if(imageFile != null) {
                    imageFile!!.delete()
                }

                val title = "회원가입 실패"
                val message = "서버와의 통신 문제로 회원가입에 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun showEmailDialog() {
        val builder = AlertDialog.Builder(this@SetProfileActivity)
        builder.setOnDismissListener {
            val intent = Intent(this@SetProfileActivity, LoginRegisterActivity::class.java)
            intent.putExtra("result", "success register")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        builder.setTitle("이메일 인증 요청")
        builder.setMessage("인증 이메일이 전송되었습니다.\n" +
                "전송된 인증 이메일 확인 후, 로그인해주세요.")

        builder.setPositiveButton("확인", null)

        builder.show()
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
}