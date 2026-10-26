package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.network.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

class ErrorMapperTest {

    @Test
    fun `mapHttpError maps 400 with details or error field`() {
        val json = """{"error":"Validation failed"}"""
        val result = ErrorMapper.mapHttpError(400, json)
        assertTrue(result.type is AppError.Validation)
        assertEquals("Validation failed", result.message)
        assertEquals("Validation failed", (result.type as AppError.Validation).backendMessage)
    }

    @Test
    fun `mapHttpError maps 400 with message field`() {
        val json = """{"message":"Invalid slot date"}"""
        val result = ErrorMapper.mapHttpError(400, json)
        assertTrue(result.type is AppError.Validation)
        assertEquals("Invalid slot date", result.message)
    }

    @Test
    fun `mapHttpError maps 401 to Unauthorized`() {
        val json = """{"error":"Token expired"}"""
        val result = ErrorMapper.mapHttpError(401, json)
        assertTrue(result.type is AppError.Unauthorized)
        assertEquals("Token expired", result.message)
    }

    @Test
    fun `mapHttpError maps 403 to Forbidden`() {
        val result = ErrorMapper.mapHttpError(403, null)
        assertTrue(result.type is AppError.Forbidden)
        assertEquals(ErrorMapper.DEFAULT_FORBIDDEN_MSG, result.message)
    }

    @Test
    fun `mapHttpError maps 404 to NotFound`() {
        val json = """{"message":"Activity not found"}"""
        val result = ErrorMapper.mapHttpError(404, json)
        assertTrue(result.type is AppError.NotFound)
        assertEquals("Activity not found", result.message)
    }

    @Test
    fun `mapHttpError maps 409 to Conflict and preserves backend message`() {
        val json = """{"error":"Slot is fully booked"}"""
        val result = ErrorMapper.mapHttpError(409, json)
        assertTrue(result.type is AppError.Conflict)
        assertEquals("Slot is fully booked", (result.type as AppError.Conflict).backendMessage)
        assertEquals("Slot is fully booked", result.message)
    }

    @Test
    fun `mapHttpError maps 500 to Server error`() {
        val result = ErrorMapper.mapHttpError(500, """{"message":"Internal Server Error"}""")
        assertTrue(result.type is AppError.Server)
        assertEquals(500, (result.type as AppError.Server).code)
    }

    @Test
    fun `mapHttpError maps bad credentials to user friendly message`() {
        val result1 = ErrorMapper.mapHttpError(401, """{"error":"Bad credentials"}""")
        assertEquals(ErrorMapper.DEFAULT_BAD_CREDENTIALS_MSG, result1.message)

        val result2 = ErrorMapper.mapHttpError(400, """{"message":"bad credentials provided"}""")
        assertEquals(ErrorMapper.DEFAULT_BAD_CREDENTIALS_MSG, result2.message)
    }

    @Test
    fun `mapThrowable maps SocketTimeoutException to Timeout`() {
        val result = ErrorMapper.mapThrowable(SocketTimeoutException("timeout"))
        assertTrue(result is ApiResult.Error)
        assertEquals(AppError.Timeout, (result as ApiResult.Error).type)
    }

    @Test
    fun `mapThrowable maps ConnectException to NoInternet`() {
        val result = ErrorMapper.mapThrowable(ConnectException("failed to connect"))
        assertTrue(result is ApiResult.Error)
        assertEquals(AppError.NoInternet, (result as ApiResult.Error).type)
    }
}
