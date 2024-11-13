package com.slackcat.app.modules.weather

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import kotlinx.serialization.Serializable

class WeatherClient {
    private val zipToGridPointsCache = mutableMapOf<String, GridPoints>()

    suspend fun getForecast(zipCode: String): CurrentForecast? {
        val gridPoints = zipToGridPointsCache[zipCode] ?: run {
            val (latitude, longitude) = getLatLonFromZip(zipCode) ?: return null
            getGridPoints(latitude, longitude)?.also {
                zipToGridPointsCache[zipCode] = it
            }
        } ?: return null

        val forecast = fetchForecast(gridPoints) ?: return null
        val currentPeriod = forecast.properties.periods.firstOrNull() ?: return null

        return CurrentForecast(
            name = currentPeriod.name,
            temperature = currentPeriod.temperature,
            temperatureUnit = currentPeriod.temperatureUnit,
            windSpeed = currentPeriod.windSpeed,
            windDirection = currentPeriod.windDirection,
            shortForecast = currentPeriod.shortForecast,
            detailedForecast = currentPeriod.detailedForecast,
            iconUrl = currentPeriod.icon
        )
    }

    private suspend fun getLatLonFromZip(zipCode: String): Pair<Double, Double>? {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$zipCode&country=US"
        val geocodeResponse = slackcatNetworkClient.fetch(url, GeocodeResponse.serializer())
        return geocodeResponse.getOrNull()?.results?.firstOrNull()?.let {
            it.latitude to it.longitude
        }
    }

    private suspend fun getGridPoints(latitude: Double, longitude: Double): GridPoints? {
        val url = "https://api.weather.gov/points/$latitude,$longitude"
        val pointResponse = slackcatNetworkClient.fetch(url, PointResponse.serializer()).getOrNull()

        return pointResponse?.properties?.let {
            GridPoints(it.gridId, it.gridX, it.gridY, it.forecast)
        }
    }

    private suspend fun fetchForecast(gridPoints: GridPoints): ForecastResponse? {
        val url = gridPoints.forecast
        val result = slackcatNetworkClient.fetch(
            url = url,
            serializer = ForecastResponse.serializer(),
            headers = mapOf("User-Agent" to "SlackcatApp")
        ).getOrNull()

        println(result)
        return result
    }
}
