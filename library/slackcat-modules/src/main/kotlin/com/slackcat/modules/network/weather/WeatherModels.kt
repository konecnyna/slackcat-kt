package com.slackcat.modules.network.weather

import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(val properties: Properties)

@Serializable
data class Properties(val periods: List<Period>)

@Serializable
data class Period(
    val name: String,
    val temperature: Int,
    val temperatureUnit: String,
    val icon: String,
    val shortForecast: String,
    val windSpeed: String,
    val windDirection: String,
    val detailedForecast: String,
)

@Serializable
data class GeocodeResponse(val results: List<Result>)

@Serializable
data class Result(val latitude: Double, val longitude: Double, val name: String)

@Serializable
data class PointResponse(val properties: PointProperties)

@Serializable
data class PointProperties(val gridId: String, val gridX: Int, val gridY: Int, val forecast: String)

data class GridPoints(val gridId: String, val gridX: Int, val gridY: Int, val forecast: String)

data class CurrentForecast(
    val name: String,
    val locationName: String,
    val temperature: Int,
    val temperatureUnit: String,
    val windSpeed: String,
    val windDirection: String,
    val shortForecast: String,
    val detailedForecast: String,
    val iconUrl: String,
)

data class LocationData(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val gridPoints: GridPoints,
)
