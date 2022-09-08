package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.poolc.linky.databinding.ActivitySearchMeBinding

class SearchMeActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchMeBinding
    private var userLinkyFragment: UserLinkyFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

        }
    }

    fun goToUserLinky(ownerEmail: String, path: String) {
        userLinkyFragment = UserLinkyFragment()

        val tran = supportFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putString("owner", "me")
        bundle.putString("email", ownerEmail)
        bundle.putString("path", path)
        userLinkyFragment!!.arguments = bundle

        tran.replace(R.id.search_me_activity_fragment_container, userLinkyFragment!!)

        tran.addToBackStack(null)

        tran.commit()
    }

    override fun onBackPressed() {
        if(userLinkyFragment != null) {
            if(userLinkyFragment!!.childFragmentManager.backStackEntryCount > 0) {
                userLinkyFragment!!.childFragmentManager.popBackStackImmediate()
            }
            else {
                userLinkyFragment = null
                super.onBackPressed()
            }
        }
        else {
            super.onBackPressed()
        }
    }
}