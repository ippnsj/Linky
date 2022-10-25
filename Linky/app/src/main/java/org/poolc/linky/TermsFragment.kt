package org.poolc.linky

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class TermsFragment : Fragment() {
    private lateinit var mainActivity : MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terms, container, false)
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    fun update() {
        mainActivity.setTopbarTitle("TermsFragment")
    }
}