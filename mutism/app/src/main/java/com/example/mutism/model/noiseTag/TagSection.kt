package com.example.mutism.model.noiseTag

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagSection(
    val category: String,
    val tags: List<String>,
) : Parcelable
