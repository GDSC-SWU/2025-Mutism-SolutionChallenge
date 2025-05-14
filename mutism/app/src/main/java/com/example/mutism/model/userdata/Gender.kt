package com.example.mutism.model.userdata

data class Gender(
    val type: String,
    val sex: String,
) {
    override fun toString(): String = sex
}
