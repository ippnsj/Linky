package org.poolc.linky

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.poolc.linky.databinding.ActivitySelectPathBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class SelectPathActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySelectPathBinding
    private var path = ""
    private lateinit var app : MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectPathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MyApplication

        with(binding) {
            // topbar 설정
            setSupportActionBar(selectPathTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            selectPathTopbarTitle.text = "경로선택"

            createFragment("")
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this@SelectPathActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    fun createFragment(newPath:String) {
        val moveCurrentPath = intent.getStringExtra("path")
        var folders : ArrayList<String>? = null
        if(moveCurrentPath == newPath) {
            folders = intent.getStringArrayListExtra("folders")
        }

        val fragment = FolderListFragment()
        val bundle = Bundle()
        bundle.putString("path", newPath)
        bundle.putStringArrayList("folders", folders)
        fragment.arguments = bundle
        setFragment(newPath, fragment)
    }

    fun setCurrentPath(path:String) {
        this.path = path
    }

    private fun setFragment(newPath: String, fragment:Fragment) {
        val tran = supportFragmentManager.beginTransaction()
        tran.replace(R.id.folderFragmentContainer, fragment)

        if(newPath != "") {
            tran.addToBackStack(null)
        }

        tran.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_select_path_topbar, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.cancel -> {
                finish()
            }
            R.id.done -> {
                if(intent.getStringExtra("purpose") == "move") {
                    val prevPath = intent.getStringExtra("path")
                    if(prevPath == path) {
                        intent.putExtra("reason", "same path")
                        setResult(RESULT_CANCELED, intent)
                        finish()
                    }
                    else {
                        val target = intent.getStringExtra("target")

                        when(target) {
                            "folder" -> {
                                val selectedFolders = intent.getStringArrayListExtra("folders")
                                val jsonObj = JSONObject()
                                jsonObj.put("originalPaths", JSONArray(selectedFolders))
                                jsonObj.put("modifiedPath", path)
                                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

                                val call = MyApplication.service.moveFolder(body)

                                call.enqueue(object: Callback<Void> {
                                    override fun onResponse(
                                        call: Call<Void>,
                                        response: Response<Void>
                                    ) {
                                        intent.putExtra("responseCode", response.code())
                                        setResult(RESULT_OK, intent)
                                        finish()
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        val title = "폴더 이동 실패"
                                        val message = "서버와의 통신 문제로 폴더 이동에 실패하였습니다.\n" +
                                                "잠시후 다시 시도해주세요."
                                        showDialog(title, message, null)
                                    }
                                })
                            }
                            "link" -> {
                                val selectedLinks = intent.getStringArrayListExtra("links")
                                val jsonObj = JSONObject()
                                jsonObj.put("originalPath", prevPath)
                                jsonObj.put("originalIds", JSONArray(selectedLinks))
                                jsonObj.put("modifiedPath", path)
                                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

                                val call = MyApplication.service.moveLink(body)

                                call.enqueue(object: Callback<Void> {
                                    override fun onResponse(
                                        call: Call<Void>,
                                        response: Response<Void>
                                    ) {
                                        intent.putExtra("responseCode", response.code())
                                        setResult(RESULT_OK, intent)
                                        finish()
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        val title = "링크 이동 실패"
                                        val message = "서버와의 통신 문제로 링크 이동에 실패하였습니다.\n" +
                                                "잠시후 다시 시도해주세요."
                                        showDialog(title, message, null)
                                    }
                                })
                            }
                        }
                    }
                }
                else {
                    intent.putExtra("path", path)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }
}