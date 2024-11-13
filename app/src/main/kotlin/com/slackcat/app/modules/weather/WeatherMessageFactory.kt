package com.slackcat.app.modules.weather

import com.slackcat.common.RichTextMessage
import com.slackcat.presentation.buildRichMessage

class WeatherMessageFactory {
    fun makeMessage(forecast: CurrentForecast): String {
        return formatForecastAsMarkdown(forecast)
    }

    private fun formatForecastAsMarkdown(forecast: CurrentForecast): String {
        return """
        *${forecast.name} Forecast*
        
        *Temperature:* ${forecast.temperature}°${forecast.temperatureUnit}
        *Wind:* ${forecast.windSpeed} from ${forecast.windDirection}
        *Condition:* ${forecast.shortForecast}
        
        *Detailed Forecast:*
        _${forecast.detailedForecast}_
    """.trimIndent()
    }

}