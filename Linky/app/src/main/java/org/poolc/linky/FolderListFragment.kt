package org.poolc.linky

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentFoldersBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class FolderListFragment : Fragment() {
    private lateinit var binding : FragmentFoldersBinding
    private val folders = ArrayList<String>()
    private lateinit var folderListAdapter : FolderListAdapter
    private lateinit var path : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        path = arguments?.getString("path")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // activity에 path값 넘김
        val activity = activity as SelectPathActivity
        activity.setCurrentPath(path)

        val jsonStr = arguments?.getString("jsonStr")
        if (jsonStr != "") {
            val jsonObj = JSONObject(jsonStr)
            val foldersArr = jsonObj.getJSONArray("folders")
            for (idx in 0 until foldersArr.length()) {
                val folderObj = foldersArr.getJSONObject(idx)
                val folderName = folderObj.getString("folderName")

                folders.add(folderName)
            }
        }

        return inflater.inflate(R.layout.fragment_folders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFoldersBinding.bind(view)

        folderListAdapter = FolderListAdapter(folders, object : FolderListAdapter.OnItemClickListener {
            override fun onItemClick(folderName: String) {
                val newPath = "${path}${folderName} / "
                val activity = activity as SelectPathActivity
                activity.createFragment(newPath)
            }
        })

        with(binding) {
            currentPath.text = path

            folderListRecycler.adapter = folderListAdapter
            folderListRecycler.addItemDecoration(DividerItemDecoration(activity, 1))

            addFolder.setOnClickListener {
                val builder = android.app.AlertDialog.Builder(activity)
                builder.setTitle("추가할 폴더명을 입력해주세요")

                val dialogView = layoutInflater.inflate(R.layout.foldername_dialog, null)
                val dialogBinding = FoldernameDialogBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                    // TODO 글자수가 1자 이상이며 10자를 넘지 않는지 확인
                    createFolder(dialogBinding.newFolderName.text.toString())
                }
                builder.setNegativeButton("취소", null)

                val dialog = builder.create()

                dialog.show()

                dialogBinding.newFolderName.setOnEditorActionListener { v, actionId, event ->
                    if(actionId == EditorInfo.IME_ACTION_DONE) {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                        true
                    }
                    false
                }
            }
        }
    }

    private fun readFolder() : String{
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_key),
            AppCompatActivity.MODE_PRIVATE
        )
        val url = URL("http://${MyApplication.ip}:${MyApplication.port}/folder/readFolder")
        var conn : HttpURLConnection? = null
        var response : String = ""

        thread {
            try {
                conn = url.openConnection() as HttpURLConnection
                conn!!.requestMethod = "POST"
                conn!!.connectTimeout = 10000;
                conn!!.readTimeout = 100000;
                conn!!.setRequestProperty("Content-Type", "application/json")
                conn!!.setRequestProperty("Accept", "application/json")

                conn!!.doOutput = true
                conn!!.doInput = true

                val body = JSONObject()
                body.put("userEmail", sharedPref!!.getString("userEmail", ""))
                body.put("path", path)

                val os = conn!!.outputStream
                os.write(body.toString().toByteArray())
                os.flush()

                if(conn!!.responseCode == 200) {
                    response = conn!!.inputStream.reader().readText()
                }
                else if(conn!!.responseCode == 400) {
                    Log.d("test", "Bad request")
                }
                else if(conn!!.responseCode == 404) {
                    Log.d("test", "Not Found")
                }
                else if(conn!!.responseCode == 401) {
                    Log.d("test", "Unauthorized")
                }
            }
            catch (e: MalformedURLException) {
                Log.d("test", "올바르지 않은 URL 주소입니다.")
            } catch (e: IOException) {
                Log.d("test", "connection 오류")
            }finally {
                conn?.disconnect()
            }
        }

        return response
    }

    private fun createFolder(folderName:String) {
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_key),
            AppCompatActivity.MODE_PRIVATE
        )
        val url = URL("http://${MyApplication.ip}:${MyApplication.port}/folder/create")
        var conn : HttpURLConnection? = null
        var response : String = ""

        thread {
            try {
                conn = url.openConnection() as HttpURLConnection
                conn!!.requestMethod = "POST"
                conn!!.connectTimeout = 10000;
                conn!!.readTimeout = 100000;
                conn!!.setRequestProperty("Content-Type", "application/json")
                conn!!.setRequestProperty("Accept", "application/json")

                conn!!.doOutput = true

                val body = JSONObject()
                body.put("userEmail", sharedPref!!.getString("userEmail", ""))
                body.put("path", path)
                body.put("folderName", folderName)

                val os = conn!!.outputStream
                os.write(body.toString().toByteArray())
                os.flush()

                if(conn!!.responseCode == 200) {
                    val jsonStr = readFolder()
                    if (jsonStr != "") {
                        folders.clear()
                        val jsonObj = JSONObject(jsonStr)
                        val foldersArr = jsonObj.getJSONArray("folders")
                        for (idx in 0 until foldersArr.length()) {
                            val folderObj = foldersArr.getJSONObject(idx)
                            val folderName = folderObj.getString("folderName")

                            folders.add(folderName)
                        }
                        folderListAdapter.notifyDataSetChanged()

                        val toast = Toast.makeText(activity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.BOTTOM, 0, 0)
                        toast.show()
                    }
                }
                else if(conn!!.responseCode == 400) {
                    Log.d("test", "Bad request")
                }
                else if(conn!!.responseCode == 404) {
                    Log.d("test", "Not Found")
                }
                else if(conn!!.responseCode == 401) {
                    Log.d("test", "Unauthorized")
                }
            }
            catch (e: MalformedURLException) {
                Log.d("test", "올바르지 않은 URL 주소입니다.")
            } catch (e: IOException) {
                Log.d("test", "connection 오류")
            }finally {
                conn?.disconnect()
            }
        }
    }
}