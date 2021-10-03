package com.example.weatherapplication.models

import java.io.Serializable

/**
 * This class takes care of the returned objects from the weather API
 */
data class WeatherResponse (
    val coordination: Coordinates,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val id: Int,
    val name: String,
    val cod: Int
): Serializable