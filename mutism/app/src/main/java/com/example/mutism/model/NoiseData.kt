package com.example.mutism.model

import com.example.mutism.R

object NoiseData {
    val list = listOf(
        NoiseItem("car", R.drawable.car),
        NoiseItem("horn", R.drawable.horn),
        NoiseItem("coffee\n" + "machine", R.drawable.coffee),
        NoiseItem("dog bark", R.drawable.dog),
        NoiseItem("vacuum\n" + "cleaner", R.drawable.vacuum),
        NoiseItem("phone\n" + "ringing", R.drawable.phone),
        NoiseItem("clock\n" + "ticking", R.drawable.clock),
        NoiseItem("door\n" + "opening", R.drawable.door),
        NoiseItem("air conditioner\n" + "/ heater", R.drawable.aircon),
        NoiseItem("fan", R.drawable.fan),
        NoiseItem("kitchen\n" + "appliance", R.drawable.kitchen),
        NoiseItem("talking in\n" + "restaurants", R.drawable.talking)
    )
}