package com.example.animepopular.data.repository

import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.data.remote.dto.MangaDexErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

abstract class BaseRepository {

    // Generic safe API call that emits Loading → Success/Error
    protected fun <T> safeApiFlow(apiCall: suspend () -> Response<T>): Flow<ApiResult<T>> =
        flow {
            emit(ApiResult.Loading)
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        emit(ApiResult.Success(body))
                    } else {
                        emit(ApiResult.Empty)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val err = errorJson.decodeFromString<MangaDexErrorResponse>(errorBody ?: "{}")
                        err.errors.firstOrNull()?.detail
                            ?: err.errors.firstOrNull()?.title
                            ?: "Unknown error"
                    } catch (e: Exception) {
                        errorBody ?: "HTTP ${response.code()}"
                    }
                    Timber.e("API Error ${response.code()}: $errorMessage")
                    emit(ApiResult.Error(response.code(), errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "Network error")
                emit(ApiResult.Error(-1, e.message ?: "Network error"))
            }
        }.flowOn(Dispatchers.IO)

    // Single safe API call (not a flow, for suspend functions)
    protected suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) ApiResult.Success(body)
                else ApiResult.Empty
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val err = errorJson.decodeFromString<MangaDexErrorResponse>(errorBody ?: "{}")
                    err.errors.firstOrNull()?.detail ?: "HTTP ${response.code()}"
                } catch (e: Exception) {
                    "HTTP ${response.code()}"
                }
                ApiResult.Error(response.code(), errorMessage)
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }
}