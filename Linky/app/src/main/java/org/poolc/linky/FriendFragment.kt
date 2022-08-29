package org.poolc.linky

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import org.poolc.linky.databinding.FragmentFriendBinding

class FriendFragment : Fragment() {
    private lateinit var binding : FragmentFriendBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication

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
        val view = inflater.inflate(R.layout.fragment_friend, container, false)
        binding = FragmentFriendBinding.bind(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            addFriend.setOnClickListener {
                showDropdown(addFriend)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    fun update() {
        mainActivity.setTopbarTitle("FriendFragment")
    }

    private fun showDropdown(view:View) {
        val popup = PopupMenu(mainActivity, view)
        popup.menuInflater.inflate(R.menu.menu_add_friend, popup.menu)
        popup.show()

        popup.setOnMenuItemClickListener(popupListener)
    }

    private val popupListener = object : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when(item?.itemId) {
                R.id.add_by_nickname -> {

                }
                R.id.add_by_kakaotalk -> {

                }
            }

            return true
        }
    }
}