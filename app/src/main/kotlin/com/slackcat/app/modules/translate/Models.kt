package com.slackcat.app.modules.translate

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse

@Serializable
data class SuccessResponse(
    val success: Success,
    val contents: Contents,
) : ApiResponse()

@Serializable
data class ErrorResponse(
    val error: Error,
) : ApiResponse()

@Serializable
data class Success(val total: Int)

@Serializable
data class Contents(val translated: String, val text: String, val translation: String)

@Serializable
data class Error(val code: Int, val message: String)
