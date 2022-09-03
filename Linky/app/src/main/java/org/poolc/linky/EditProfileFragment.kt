package org.poolc.linky

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import org.poolc.linky.databinding.DialogChangeProfileImageBinding
import org.poolc.linky.databinding.FragmentEditProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class EditProfileFragment : Fragment() {
    private lateinit var binding : FragmentEditProfileBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
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
    private var imageUrl = ""
    private var imageChange = false
    private var uri : Uri? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        binding = FragmentEditProfileBinding.bind(view)

        albumResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if(result.resultCode == AppCompatActivity.RESULT_OK) {
                    uri = result.data?.data

                    if(uri != null) {
                        imageChange = true

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // 안드로이드 10 버전부터
                            val source = ImageDecoder.createSource(mainActivity.contentResolver, uri!!)
                            mimetype = mainActivity.contentResolver.getType(uri!!).toString()
                            bitmap = ImageDecoder.decodeBitmap(source)
                            binding.editProfileImage.setImageBitmap(bitmap)
                        }
                        else {
                            // 안드로이드 9 버전까지
                            val cursor = mainActivity.contentResolver.query(uri!!, null, null, null, null)
                            if(cursor != null) {
                                cursor.moveToNext()
                                // 이미지 경로를 가져온다.
                                val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                                val source = cursor.getString(index)
                                // 이미지를 생성한다.
                                bitmap = BitmapFactory.decodeFile(source)
                                binding.editProfileImage.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
            }
            else {
                val builder = AlertDialog.Builder(mainActivity)

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
                    mainActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Handler(Looper.getMainLooper()).postDelayed({
                    pickImageFromGallary()
                }, 200)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            editProfileImage.setOnClickListener {
                val intent = Intent(mainActivity, ProfileImageActivity::class.java)
                intent.putExtra("imageUrl", imageUrl)
                intent.putExtra("uri", uri)
                startActivity(intent)
            }

            changeProfileImage.setOnClickListener {
                val builder = AlertDialog.Builder(mainActivity)

                val custom_view = layoutInflater.inflate(R.layout.dialog_change_profile_image, null)

                builder.setView(custom_view)

                val dialog = builder.create()

                val dialogBinding = DialogChangeProfileImageBinding.bind(custom_view)
                dialogBinding.pickAlbum.setOnClickListener {
                    checkImagePermission()

                    dialog.dismiss()
                }

                dialogBinding.pickDefault.setOnClickListener {
                    uri = null
                    imageChange = true
                    imageUrl = ""
                    bitmap = null
                    editProfileImage.setImageResource(R.drawable.profile)

                    dialog.dismiss()
                }

                dialog.show()
            }

            saveProfile.setOnClickListener {
                editProfile(nicknameTextInput.text.toString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    fun update() {
        mainActivity.setTopbarTitle("EditProfileFragment")

        thread {
            val jsonStr = app.getProfile()
            if(jsonStr != "") {
                val jsonObj = JSONObject(jsonStr)
                imageUrl = jsonObj.getString("imageUrl")

                mainActivity.runOnUiThread {
                    binding.nicknameTextInput.setText(jsonObj.getString("nickname"))
                    binding.email.text = jsonObj.getString("email")
                }

                if (!imageChange && imageUrl != "") {
                    try {
                        val url: URL? = URL(imageUrl)
                        val conn: HttpURLConnection? =
                            url?.openConnection() as HttpURLConnection
                        val image = BitmapFactory.decodeStream(conn?.inputStream)
                        mainActivity.runOnUiThread {
                            binding.editProfileImage.setImageBitmap(image)
                        }
                    }
                    catch (e:Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun editProfile(newNickname:String) {
        val newNicknameTrim = newNickname.trim()
        val pattern = Pattern.compile("^[ㄱ-ㅣ가-힣a-zA-Z\\s]+$")

        if(newNicknameTrim == "") {
            val builder = AlertDialog.Builder(mainActivity)
            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("프로필 수정 실패")
            builder.setMessage("닉네임은 앞/뒤 공백 없이 1자 이상 입력해주세요.")
            builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                binding.nicknameTextInput.setText("")
            }
            builder.show()
        }
        else if(!pattern.matcher(newNicknameTrim).matches()) {
            val builder = AlertDialog.Builder(mainActivity)
            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("프로필 수정 실패")
            builder.setMessage("닉네임은 한글, 영어, 사이 공백만 가능합니다.")
            builder.setPositiveButton("확인", null)
            builder.show()
        }
        else {
            if (bitmap != null) {
                createTempImageFile(mimetype, bitmap!!)
            }

            val email = binding.email.text.toString()
            val emailBody = RequestBody.create(MediaType.parse("text/plain"), email)
            val newNicknameBody = RequestBody.create(MediaType.parse("text/plain"), newNicknameTrim)
            val imageChangeBody = RequestBody.create(MediaType.parse("text/plain"), imageChange.toString())
            var multipartBody: MultipartBody.Part? = null

            if(imageFile != null) {
                val requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile)

                multipartBody = MultipartBody.Part.createFormData(
                    "newMultipartFile",
                    imageFile!!.name,
                    requestFile
                )
            }

            val body = HashMap<String, RequestBody>()
            body["email"] = emailBody
            body["newNickname"] = newNicknameBody
            body["imageChange"] = imageChangeBody

            thread {
                val call = MyApplication.service.editProfile(body, multipartBody)

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
                                mainActivity.runOnUiThread {
                                    val toast = Toast.makeText(
                                        mainActivity,
                                        "프로필 수정이 완료되었습니다~!",
                                        Toast.LENGTH_SHORT
                                    )
                                    toast.show()
                                    mainActivity.onBackPressed()
                                }
                            }
                            else {
                                mainActivity.runOnUiThread {
                                    val builder = AlertDialog.Builder(mainActivity)

                                    builder.setMessage("이미지 파일을 가져오는데 실패하여 기존 이미지로 저장됩니다.")

                                    builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                                        val toast = Toast.makeText(
                                            mainActivity,
                                            "프로필 수정이 완료되었습니다~!",
                                            Toast.LENGTH_SHORT
                                        )
                                        toast.show()
                                        mainActivity.onBackPressed()
                                    }

                                    builder.show()
                                }
                            }
                        } else {
                            var positiveButtonFunc: DialogInterface.OnClickListener? = null
                            var message = ""

                            when (response.code()) {
                                401 -> {
                                    message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                                    positiveButtonFunc = object : DialogInterface.OnClickListener {
                                        override fun onClick(dialog: DialogInterface?, which: Int) {
                                            val editSharedPref = MyApplication.sharedPref.edit()
                                            editSharedPref.remove("email").apply()

                                            val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                            startActivity(intent)
                                        }
                                    }
                                }
                                404 -> {
                                    message = "해당 유저가 존재하지 않습니다.\n" +
                                            "자동 로그아웃 됩니다."
                                    positiveButtonFunc = object : DialogInterface.OnClickListener {
                                        override fun onClick(dialog: DialogInterface?, which: Int) {
                                            val editSharedPref = MyApplication.sharedPref.edit()
                                            editSharedPref.remove("email").apply()

                                            val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                            startActivity(intent)
                                        }
                                    }
                                }
                                409 -> {
                                    binding.nicknameTextInput.error = "이미 해당 닉네임이 존재합니다."

                                    message = "이미 해당 닉네임이 존재합니다.\n" +
                                            "닉네임을 바꿔주세요."
                                }
                                else -> {
                                    message = "서버 문제로 프로필 수정에 실패하였습니다.\n" +
                                            "잠시후 다시 시도해주세요."
                                }
                            }

                            mainActivity.runOnUiThread {
                                val builder = AlertDialog.Builder(mainActivity)

                                builder.setIcon(R.drawable.ic_baseline_warning_8)
                                builder.setTitle("프로필 수정 실패")
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

                        mainActivity.runOnUiThread {
                            val builder = AlertDialog.Builder(mainActivity)

                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("프로필 수정 실패")
                            builder.setMessage("서버 문제로 프로필 수정에 실패하였습니다.\n" +
                                    "잠시후 다시 시도해주세요.")

                            builder.setPositiveButton("확인", null)

                            builder.show()
                        }
                    }
                })
            }
        }
    }

    private fun createTempImageFile(mimetype:String, bitmap:Bitmap) {
        var type = mimetype.substring(mimetype.lastIndexOf("/") + 1, mimetype.length)
        if(type != "jpeg" || type != "png") {
            type = "jpeg"
        }

        val file_name = "temp_${System.currentTimeMillis()}.$type"

        imageFile = File.createTempFile(file_name, null, mainActivity.cacheDir)

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
        if(ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallary()
        }
        else if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val builder = AlertDialog.Builder(mainActivity)

            builder.setMessage("'파일 및 미디어' 권한이 허용되어야 이미지 변경이 가능합니다.\n" +
                    "아래 설정 버튼을 눌러 '파일 및 미디어' 권한을 허용해주세요.")

            builder.setPositiveButton("설정") { dialogInterface: DialogInterface, i: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:${mainActivity.packageName}"))
                permissionResultLauncher.launch(intent)
            }

            builder.setNegativeButton("취소", null)

            builder.show()
        }
        else {
            mainActivity.requestPermissions(permissionList, 0)
        }
    }
}