package com.example.mutism.model

data class Gender(
    val type: String,
    val sex: String,
) {
    override fun toString(): String = sex
}
