package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentEditProfileBinding
import java.util.regex.Pattern
import kotlin.concurrent.thread

class EditProfileFragment : Fragment() {
    private lateinit var binding : FragmentEditProfileBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
    private var needImageUrl = false
    private var imageUrl = ""

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            editProfileImage.setOnClickListener {
                val intent = Intent(mainActivity, ProfileImageActivity::class.java)
                intent.putExtra("imageUrl", imageUrl)
                startActivity(intent)
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

    private fun getImageUrl() : String {
        return ""
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
            thread {
                if (needImageUrl) {
                    imageUrl = getImageUrl()
                }

                val responseCode = app.editProfile(newNicknameTrim, imageUrl)

                when(responseCode) {
                    200 -> {
                        mainActivity.runOnUiThread {
                            val toast = Toast.makeText(mainActivity, "프로필 수정이 완료되었습니다~!", Toast.LENGTH_SHORT)
                            toast.show()
                            mainActivity.onBackPressed()
                        }
                    }
                    400 -> {
                        // TODO 해당 이메일의 유저가 없음? 401과 차이는?
                    }
                    401 -> {
                        val builder = AlertDialog.Builder(mainActivity)
                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("프로필 수정 실패")
                        builder.setMessage("사용자 인증 오류로 인해 자동 로그아웃 됩니다.")
                        builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                            mainActivity.finishAffinity()
                            System.exit(0)
                        }
                        builder.show()
                    }
                }
            }
        }
    }

    fun update() {
        mainActivity.setTopbarTitle("EditProfileFragment")

        thread {
            val jsonStr = app.getProfile()
            val jsonObj = JSONObject(jsonStr)
            imageUrl = jsonObj.getString("imageUrl")

            if(imageUrl != "") {
                // TODO url로부터 이미지 가져오는 작업
            }

            mainActivity.runOnUiThread {
                with(binding) {
                    nicknameTextInput.setText(jsonObj.getString("nickname"))
                    // TODO api 수정돼서 email 넘어오면 수정
                    email.text = MyApplication.sharedPref.getString("email", "")

                    if(imageUrl != "") {
                        // TODO url로부터 가져온 이미지 display
                    }
                }
            }
        }
    }
}