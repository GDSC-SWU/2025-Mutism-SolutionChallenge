package com.example.mutism.controller.noiseSelectPage

interface TagSelectionListener {
    fun onTagSelected(tag: String)

    fun onTagDeselected(tag: String)
}
