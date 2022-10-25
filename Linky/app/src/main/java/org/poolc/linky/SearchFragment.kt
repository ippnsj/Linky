package org.poolc.linky

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.poolc.linky.databinding.FragmentSearchBinding
import org.poolc.linky.viewmodel.SearchViewModel

class SearchFragment : Fragment() {
    private lateinit var binding : FragmentSearchBinding
    private lateinit var mainActivity: MainActivity
    private val model: SearchViewModel by activityViewModels()

    private val fragmentList = ArrayList<Fragment>()
    private val tabText = ArrayList<String>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentList.add(SearchResultFolderFragment())
        tabText.add("폴더")
        fragmentList.add(SearchResultLinkFragment())
        tabText.add("링크")
        fragmentList.add(SearchResultUserFragment())
        tabText.add("유저")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        binding = FragmentSearchBinding.bind(view)

        val pagerAdapter = object : FragmentStateAdapter(mainActivity) {
            override fun getItemCount(): Int {
                return fragmentList.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragmentList[position]
            }
        }

        with(binding) {
            tabs.setSelectedTabIndicatorColor(Color.parseColor("#707070"))

            pager.adapter = pagerAdapter

            TabLayoutMediator(tabs, pager) { tab: TabLayout.Tab, i: Int ->
                tab.text = tabText[i]
            }.attach()

            searchView.setOnClickListener {
                searchView.isIconified = false
            }

            val queryTextListener = object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    model.updateSearchText(newText ?: "")

                    return true
                }

            }

            searchView.setOnQueryTextListener(queryTextListener)
        }

        return view
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if(!hidden) {
            val tabIdx = binding.tabs.selectedTabPosition

            when(tabIdx) {
                0 -> {
                    val fragment = fragmentList[tabIdx] as SearchResultFolderFragment
                    fragment.update()
                }
                1 -> {
                    val fragment = fragmentList[tabIdx] as SearchResultLinkFragment
                    fragment.update()
                }
                2 -> {
                    val fragment = fragmentList[tabIdx] as SearchResultUserFragment
                    fragment.update()
                }
            }
        }
    }
}