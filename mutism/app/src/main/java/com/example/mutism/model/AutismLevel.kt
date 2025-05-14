package com.example.mutism.model

data class AutismLevel(
    val type: String,
    val level: Int,
) {
    override fun toString(): String = type
}
