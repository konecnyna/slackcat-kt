package com.slackcat.app.modules.weather

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import kotlinx.serialization.Serializable

class WeatherClient {
    private val zipToLocationDataCache = mutableMapOf<String, LocationData>()

    suspend fun getForecast(zipCode: String): CurrentForecast? {
        // Retrieve location data from cache or fetch if not available
        val locationData = zipToLocationDataCache[zipCode] ?: run {
            val (locationName, latitude, longitude) = getLatLonFromZip(zipCode) ?: return null
            val gridPoints = getGridPoints(latitude, longitude) ?: return null
            LocationData(locationName, latitude, longitude, gridPoints).also {
                zipToLocationDataCache[zipCode] = it
            }
        }

        val forecast = fetchForecast(locationData.gridPoints) ?: return null
        val currentPeriod = forecast.properties.periods.firstOrNull() ?: return null

        return CurrentForecast(
            name = currentPeriod.name,
            locationName = locationData.locationName,
            temperature = currentPeriod.temperature,
            temperatureUnit = currentPeriod.temperatureUnit,
            windSpeed = currentPeriod.windSpeed,
            windDirection = currentPeriod.windDirection,
            shortForecast = currentPeriod.shortForecast,
            detailedForecast = currentPeriod.detailedForecast,
            iconUrl = currentPeriod.icon
        )
    }

    private suspend fun getLatLonFromZip(zipCode: String): Triple<String, Double, Double>? {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$zipCode&country=US"
        val geocodeResponse = slackcatNetworkClient.fetch(url, GeocodeResponse.serializer())
        return geocodeResponse.getOrNull()?.results?.firstOrNull()?.let {
            Triple(it.name ?: "Unknown Location", it.latitude, it.longitude)
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