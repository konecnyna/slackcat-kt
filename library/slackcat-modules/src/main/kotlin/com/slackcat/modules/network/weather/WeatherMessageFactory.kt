package com.slackcat.modules.network.weather

class WeatherMessageFactory {
    fun makeMessage(forecast: CurrentForecast): String {
        return formatForecastAsMarkdown(forecast)
    }

    private fun formatForecastAsMarkdown(forecast: CurrentForecast): String {
        return """
            *${forecast.name} Forecast for ${forecast.locationName}*
            
            *Temperature:* ${forecast.temperature}°${forecast.temperatureUnit}
            *Wind:* ${forecast.windSpeed} from ${forecast.windDirection}
            *Condition:* ${forecast.shortForecast}
            
            *Detailed Forecast:*
            _${forecast.detailedForecast}_
            """.trimIndent()
    }
}
