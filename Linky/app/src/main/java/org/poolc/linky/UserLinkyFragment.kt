package org.poolc.linky

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentUserLinkyBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class UserLinkyFragment : Fragment() {
    private lateinit var binding: FragmentUserLinkyBinding
    private lateinit var activity: Activity
    private lateinit var app: MyApplication

    private lateinit var owner : String
    private lateinit var email : String
    private var imageUrl = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as Activity
        app = activity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        owner = arguments?.getString("owner") ?: ""
        email = arguments?.getString("email") ?: ""
        val path = arguments?.getString("path") ?: ""

        setFragment(path, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_linky, container, false)
        binding = FragmentUserLinkyBinding.bind(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            userImage.setOnClickListener {
                val intent = Intent(activity, ProfileImageActivity::class.java)
                intent.putExtra("imageUrl", imageUrl)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun update() {
        when(owner) {
            "me" -> {
                binding.followButton.visibility = View.GONE
            }
            "other" -> {
                binding.followButton.visibility = View.VISIBLE
            }
        }

        getUserProfile()
    }

    private fun getUserProfile() {
        thread {
            val myEmail = MyApplication.sharedPref.getString("email", "")
            val call = MyApplication.service.getUserProfile(myEmail!!, email)

            call.enqueue(object : Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    if(response.isSuccessful) {
                        setUserProfile(response.body()!!.asJsonObject)
                    }
                    else {
                        // TODO 에러코드 세분화
                        activity.runOnUiThread {
                            val builder = AlertDialog.Builder(activity)
                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("유저 정보 불러오기 실패")
                            builder.setMessage("유저 정보를 불러오는데 실패하여 이전으로 돌아갑니다.")
                            builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                                activity.finish()
                            }
                            builder.show()
                        }
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    activity.runOnUiThread {
                        val builder = AlertDialog.Builder(activity)
                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("유저 정보 불러오기 실패")
                        builder.setMessage("유저 정보를 불러오는데 실패하여 이전으로 돌아갑니다.")
                        builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                            activity.finish()
                        }
                        builder.show()
                    }
                }
            })
        }
    }

    private fun setUserProfile(jsonObj: JsonObject) {
        imageUrl = ""

        if(!jsonObj.isJsonNull) {
            val following = jsonObj.get("following").asString
            val follower = jsonObj.get("follower").asString
            val nickname = jsonObj.get("nickname").asString
            val isFollowing = jsonObj.get("isFollowing").asString
            imageUrl = jsonObj.get("imageUrl").asString

            if(isFollowing.toBoolean()) {
                binding.followButton.text = "팔로우 취소"
                binding.followButton.setOnClickListener {
                    unfollow()
                }
            }
            else {
                binding.followButton.text = "팔로우 하기"
                binding.followButton.setOnClickListener {
                    follow()
                }
            }

            binding.userNickname.text = nickname
            binding.followInfo.text = "팔로잉 $following 팔로워 $follower"

            thread {
                if (imageUrl != "") {
                    try {
                        val url: URL? = URL(imageUrl)
                        val conn: HttpURLConnection? =
                            url?.openConnection() as HttpURLConnection
                        val image = BitmapFactory.decodeStream(conn?.inputStream)
                        activity.runOnUiThread {
                            binding.userImage.setImageBitmap(image)
                        }
                    } catch (e: Exception) {
                        activity.runOnUiThread {
                            binding.userImage.setImageResource(R.drawable.profile)
                        }
                        e.printStackTrace()
                    }
                }
                else {
                    activity.runOnUiThread {
                        binding.userImage.setImageResource(R.drawable.profile)
                    }
                }
            }
        }
    }

    private fun unfollow() {
        thread {
            val myEmail = MyApplication.sharedPref.getString("email", "")

            val jsonObj = JSONObject()
            jsonObj.put("email", myEmail)
            jsonObj.put("followedEmail", email)
            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

            val call = MyApplication.service.unfollowByEmail(body)

            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if(response.isSuccessful) {
                        activity.runOnUiThread {
                            val toast = Toast.makeText(activity, "언팔로우에 성공하였습니다~!", Toast.LENGTH_SHORT)
                            toast.show()
                            update()
                        }
                    }
                    else {
                        var message = ""
                        var positiveButtonFunc: DialogInterface.OnClickListener? = null

                        when(response.code()) {
                            400, 401 -> {
                                message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                                positiveButtonFunc = object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        val editSharedPref = MyApplication.sharedPref.edit()
                                        editSharedPref.remove("email").apply()

                                        val intent =
                                            Intent(activity, LoginRegisterActivity::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                    }
                                }
                            }
                            404 -> {
                                message = "해당 유저가 존재하지 않습니다."
                                positiveButtonFunc = object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        activity.finish()
                                    }
                                }
                            }
                        }

                        activity.runOnUiThread {
                            val builder = AlertDialog.Builder(activity)

                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("언팔로우 실패")
                            builder.setMessage(message)

                            builder.setPositiveButton("확인", positiveButtonFunc)

                            builder.show()
                        }
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    activity.runOnUiThread {
                        val builder = AlertDialog.Builder(activity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("언팔로우 실패")
                        builder.setMessage("서버 문제로 언팔로우에 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요.")

                        builder.setPositiveButton("확인", null)

                        builder.show()
                    }
                }
            })
        }
    }

    private fun follow() {
        thread {
            val myEmail = MyApplication.sharedPref.getString("email", "")

            val jsonObj = JSONObject()
            jsonObj.put("email", myEmail)
            jsonObj.put("followedEmail", email)
            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

            val call = MyApplication.service.followByEmail(body)

            call.enqueue(object : Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    if(response.isSuccessful) {
                        activity.runOnUiThread {
                            val toast = Toast.makeText(activity, "팔로우에 성공하였습니다~!", Toast.LENGTH_SHORT)
                            toast.show()
                            update()
                        }
                    }
                    else {
                        var message = ""
                        var positiveButtonFunc: DialogInterface.OnClickListener? = null

                        when(response.code()) {
                            400, 401 -> {
                                message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                                positiveButtonFunc = object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        val editSharedPref = MyApplication.sharedPref.edit()
                                        editSharedPref.remove("email").apply()

                                        val intent =
                                            Intent(activity, LoginRegisterActivity::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                    }
                                }
                            }
                            404 -> {
                                message = "해당 유저가 존재하지 않습니다."
                                positiveButtonFunc = object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        activity.finish()
                                    }
                                }
                            }
                            409 -> {
                                message = "이미 팔로우 중입니다."
                            }
                            417 -> {
                                message = "안타깝게도 자기 자신을 팔로우할 수는 없습니다..."
                            }
                        }

                        activity.runOnUiThread {
                            val builder = AlertDialog.Builder(activity)

                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("팔로우 실패")
                            builder.setMessage(message)

                            builder.setPositiveButton("확인", positiveButtonFunc)

                            builder.show()
                        }
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    activity.runOnUiThread {
                        val builder = AlertDialog.Builder(activity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("팔로우 실패")
                        builder.setMessage("서버 문제로 팔로우에 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요.")

                        builder.setPositiveButton("확인", null)

                        builder.show()
                    }
                }
            })
        }
    }

    fun setFragment(path: String, addToBackStack: Boolean) {
        var fragment : Fragment? = null

        when(path) {
            "" -> {
                fragment = UserLinkyOutsideFolderFragment()
            }
            else -> {
                fragment = UserLinkyInsideFolderFragment()
            }
        }

        val tran = childFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putString("owner", owner)
        bundle.putString("email", email)
        bundle.putString("path", path)
        fragment.arguments = bundle

        tran.replace(R.id.user_linky_fragment_container, fragment)

        if(addToBackStack) {
            tran.addToBackStack(null)
        }

        tran.commit()
    }
}