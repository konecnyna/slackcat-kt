package com.slackcat.app.modules.translate

import kotlinx.serialization.Serializable

@Serializable
sealed class FunTranslationApiResponse

@Serializable
data class SuccessResponseFunTranslation(
    val success: Success,
    val contents: Contents,
) : FunTranslationApiResponse()

@Serializable
data class ErrorResponseFunTranslation(
    val error: Error,
) : FunTranslationApiResponse()

@Serializable
data class Success(val total: Int)

@Serializable
data class Contents(val translated: String, val text: String, val translation: String)

@Serializable
data class Error(val code: Int, val message: String)
