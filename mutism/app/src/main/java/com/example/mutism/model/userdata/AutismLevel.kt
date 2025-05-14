package com.example.mutism.model.userdata

data class AutismLevel(
    val type: String,
    val level: Int,
) {
    override fun toString(): String = type
}
