package org.poolc.linky

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(activity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("??????", null)

        builder.show()
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
        val call = MyApplication.service.getUserProfile(email)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setUserProfile(response.body()!!.asJsonObject)
                }
                else {
                    val title = "?????? ?????? ???????????? ??????"
                    val message = "?????? ????????? ?????? ?????? ????????? ??????????????? ???????????? ???????????? ???????????????."
                    val listener = DialogInterface.OnDismissListener { activity.finish() }
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                val title = "?????? ?????? ???????????? ??????"
                val message = "???????????? ?????? ????????? ?????? ????????? ??????????????? ???????????? ???????????? ???????????????."
                val listener = DialogInterface.OnDismissListener { activity.finish() }
                showDialog(title, message, listener)
            }
        })
    }

    private fun setUserProfile(jsonObj: JsonObject) {
        imageUrl = ""

        if(!jsonObj.isJsonNull) {
            val following = jsonObj.get("following").asString
            val follower = jsonObj.get("follower").asString
            val nickname = jsonObj.get("nickname").asString
            val isFollowing = jsonObj.get("isFollowing").asBoolean
            imageUrl = jsonObj.get("imageUrl").asString

            if(isFollowing) {
                binding.followButton.text = "????????? ??????"
                binding.followButton.setOnClickListener {
                    unfollow()
                }
            }
            else {
                binding.followButton.text = "?????????"
                binding.followButton.setOnClickListener {
                    follow()
                }
            }

            binding.userNickname.text = nickname
            binding.followInfo.text = "????????? $following ????????? $follower"

            if (imageUrl != "") {
                thread {
                    val image = app.getImageUrl(imageUrl)

                    if (image != null) {
                        activity.runOnUiThread {
                            binding.userImage.setImageBitmap(image)
                        }
                    } else {
                        activity.runOnUiThread {
                            binding.userImage.setImageResource(R.drawable.profile)
                        }
                    }
                }
            }
            else {
                binding.userImage.setImageResource(R.drawable.profile)
            }
        }
    }

    private fun unfollow() {
        val jsonObj = JSONObject()
        jsonObj.put("followedEmail", email)
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

        val call = MyApplication.service.unfollowByEmail(body)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if(response.isSuccessful) {
                    val toast = Toast.makeText(activity, "????????? ????????? ?????????????????????~!", Toast.LENGTH_SHORT)
                    toast.show()
                    update()
                }
                else {
                    val title = "????????? ?????? ??????"
                    var message = "?????? ????????? ?????? ????????? ????????? ?????????????????????."
                    var listener: DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "?????? ????????? ?????????????????? ????????? ?????? ????????? ???????????? ????????????."
                            listener = DialogInterface.OnDismissListener {
                                activity.finish()
                            }
                        }
                    }

                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                val title = "????????? ?????? ??????"
                val message = "???????????? ?????? ????????? ????????? ????????? ?????????????????????.\n" +
                        "????????? ?????? ??????????????????."
                showDialog(title, message, null)
            }
        })
    }

    private fun follow() {
        val jsonObj = JSONObject()
        jsonObj.put("followedEmail", email)
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

        val call = MyApplication.service.followByEmail(body)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    val toast = Toast.makeText(activity, "???????????? ?????????????????????~!", Toast.LENGTH_SHORT)
                    toast.show()
                    update()
                }
                else {
                    val title = "????????? ??????"
                    var message = "?????? ????????? ?????? ???????????? ?????????????????????."
                    var listener: DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "?????? ????????? ???????????? ????????????."
                            listener = DialogInterface.OnDismissListener {
                                activity.finish()
                            }
                        }
                        409 -> {
                            message = "?????? ????????? ????????????."
                        }
                        417 -> {
                            message = "??????????????? ?????? ????????? ???????????? ?????? ????????????..."
                        }
                    }

                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                val title = "????????? ??????"
                val message = "???????????? ?????? ????????? ???????????? ?????????????????????.\n" +
                        "????????? ?????? ??????????????????."
                showDialog(title, message, null)
            }
        })
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