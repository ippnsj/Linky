package org.poolc.linky

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.poolc.linky.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var path = ""
    private var folderName = ""
    private var linkyFragment : LinkyFragment? = null
    private var searchFragment : SearchFragment? =null
    private var moreFragment : MoreFragment? = null
    private var parentFragment : Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // topbar 설정
            setSupportActionBar(topbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            topbarTitle.text = "내 링키"

            // bottom navigatonbar 설정
            bottomNavigation.itemIconTintList = null
            bottomNavigation.setOnItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.linky -> {
                        moveToLinky()
                    }
                    R.id.search -> {
                        moveToSearch()
                    }
                    R.id.more -> {
                        moveToMore()
                    }
                }
                true
            }
            bottomNavigation.selectedItemId = R.id.linky

            // intent 처리
            when(intent.getStringExtra("from")) {
                "add" -> {
                    val toast = Toast.makeText(this@MainActivity, "새로운 링키가 추가되었습니다~!", Toast.LENGTH_SHORT)
                    toast.show()
                }
                "login" -> {
                    val toast = Toast.makeText(this@MainActivity, "로그인되었습니다~!", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }

    private fun moveToLinky() {
        val fragment = supportFragmentManager.findFragmentByTag("linky")

        if(fragment == null) {
            linkyFragment = LinkyFragment()
            parentFragment = linkyFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.folderFragmentContainer, linkyFragment!!, "linky")
                .commit()
        }
        else {
            parentFragment = linkyFragment
            supportFragmentManager.beginTransaction().show(fragment).commit()
        }

        if(searchFragment != null) supportFragmentManager.beginTransaction().hide(supportFragmentManager.findFragmentByTag("search")!!).commit()
        if(moreFragment != null) supportFragmentManager.beginTransaction().hide(supportFragmentManager.findFragmentByTag("more")!!).commit()
    }

    private fun moveToSearch() {
        val fragment = supportFragmentManager.findFragmentByTag("search")

        if(fragment == null) {
            binding.topbarTitle.text = "검색"
            binding.topbarTitle.visibility = View.VISIBLE
            binding.topbarFoldername.visibility = View.INVISIBLE

            searchFragment = SearchFragment()
            parentFragment = searchFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.folderFragmentContainer, searchFragment!!, "search")
                .commit()
        }
        else {
            parentFragment = searchFragment
            supportFragmentManager.beginTransaction().show(fragment).commit()
        }

        if(linkyFragment != null) supportFragmentManager.beginTransaction().hide(supportFragmentManager.findFragmentByTag("linky")!!).commit()
        if(moreFragment != null) supportFragmentManager.beginTransaction().hide(supportFragmentManager.findFragmentByTag("more")!!).commit()
    }

    private fun moveToMore() {
        val fragment = supportFragmentManager.findFragmentByTag("more")

        if(fragment == null) {
            moreFragment = MoreFragment()
            parentFragment = moreFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.folderFragmentContainer, moreFragment!!, "more")
                .commit()
        }
        else {
            parentFragment = moreFragment
            supportFragmentManager.beginTransaction().show(fragment).commit()
        }

        if(linkyFragment != null) supportFragmentManager.beginTransaction().hide(supportFragmentManager.findFragmentByTag("linky")!!).commit()
        if(searchFragment != null) supportFragmentManager.beginTransaction().hide(supportFragmentManager.findFragmentByTag("search")!!).commit()
    }

    fun changeChildFragment(fragment:Fragment, bundle:Bundle?, addToStack:Boolean) {
        val tran = parentFragment!!.childFragmentManager.beginTransaction()

        if(bundle != null) {
            fragment.arguments = bundle
        }

        if(parentFragment is LinkyFragment) {
            tran.replace(R.id.linky_fragment_container, fragment)
        }
        else if(parentFragment is SearchFragment) {

        }
        else if(parentFragment is MoreFragment) {
            tran.replace(R.id.more_fragment_container, fragment)
        }

        if(addToStack) {
            tran.addToBackStack(null)
        }

        tran.commit()
    }

    fun setPath(path:String) {
        this.path = path
    }

    fun setFolderName(folderName:String) {
        this.folderName = folderName
    }

    fun setTopbarTitle(fragment:String) {
        if(parentFragment is LinkyFragment) {
            when(fragment) {
                "LinkyOutsideFolderFragment" -> {
                    binding.topbarTitle.text = "내 링키"
                    binding.topbarTitle.visibility = View.VISIBLE
                    binding.topbarFoldername.visibility = View.INVISIBLE
                }
                "LinkyInsideFolderFragment" -> {
                    binding.topbarFoldername.text = folderName
                    binding.topbarTitle.visibility = View.INVISIBLE
                    binding.topbarFoldername.visibility = View.VISIBLE
                }
            }
        }
        else if(parentFragment is SearchFragment) {
            binding.topbarTitle.text = "검색"
            binding.topbarTitle.visibility = View.VISIBLE
            binding.topbarFoldername.visibility = View.INVISIBLE
        }
        else if(parentFragment is MoreFragment) {
            when(fragment) {
                "InformationFragment" -> {
                    binding.topbarTitle.text = "설정"
                    binding.topbarTitle.visibility = View.VISIBLE
                    binding.topbarFoldername.visibility = View.INVISIBLE
                }
                "EditProfileFragment" -> {
                    binding.topbarFoldername.text = "프로필 수정"
                    binding.topbarTitle.visibility = View.INVISIBLE
                    binding.topbarFoldername.visibility = View.VISIBLE
                }
                "FollowingFragment" -> {
                    binding.topbarFoldername.text = "팔로잉 관리"
                    binding.topbarTitle.visibility = View.INVISIBLE
                    binding.topbarFoldername.visibility = View.VISIBLE
                }
                "FollowerFragment" -> {
                    binding.topbarFoldername.text = "팔로워 관리"
                    binding.topbarTitle.visibility = View.INVISIBLE
                    binding.topbarFoldername.visibility = View.VISIBLE
                }
                "TermsFragment" -> {
                    binding.topbarFoldername.text = "개인정보 처리방침 및 서비스 이용약관"
                    binding.topbarTitle.visibility = View.INVISIBLE
                    binding.topbarFoldername.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onBackPressed() {
        if(parentFragment!!.childFragmentManager.backStackEntryCount > 0) {
            parentFragment!!.childFragmentManager.popBackStackImmediate()
        }
        else {
            super.onBackPressed()
        }
    }
}