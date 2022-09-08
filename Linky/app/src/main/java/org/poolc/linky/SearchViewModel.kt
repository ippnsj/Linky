package org.poolc.linky

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel() : ViewModel() {
    private var _searchText = MutableLiveData<String>()
    val searchText: LiveData<String> get() = _searchText

    init {
        _searchText.value = ""
    }

    fun updateSearchText(searchText: String) {
        _searchText.value = searchText
    }
}