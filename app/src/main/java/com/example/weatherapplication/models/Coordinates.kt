package com.example.weatherapplication.models

import java.io.Serializable

data class Coordinates(
    val long: Double,
    val lat: Double
) : Serializable