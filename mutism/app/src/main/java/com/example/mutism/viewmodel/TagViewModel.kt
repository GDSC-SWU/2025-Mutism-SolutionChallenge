package com.example.mutism.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TagViewModel : ViewModel() {
    private val _selectedTags = MutableLiveData<Set<String>>(emptySet())
    val selectedTags: LiveData<Set<String>> get() = _selectedTags

    fun selectTag(tag: String) {
        val updated = _selectedTags.value.orEmpty().toMutableSet()
        updated.add(tag)
        _selectedTags.value = updated
    }

    fun deselectTag(tag: String) {
        val updated = _selectedTags.value.orEmpty().toMutableSet()
        updated.remove(tag)
        _selectedTags.value = updated
    }
}
