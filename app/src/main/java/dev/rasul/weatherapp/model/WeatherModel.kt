package dev.rasul.weatherapp.model

import com.google.gson.Gson
import dev.rasul.weatherapp.api.WeatherService
import dev.rasul.weatherapp.db.WeatherDao
import dev.rasul.weatherapp.dto.ErrorDto
import dev.rasul.weatherapp.entity.PlacesEntity
import dev.rasul.weatherapp.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WeatherModel @Inject constructor(
    private val weatherDao: WeatherDao,
    private val weatherService: WeatherService
) {
    suspend fun getWeather(
        lat: Double = 40.3777,
        lon: Double = 49.892
    ): Flow<Result<WeatherEntity?>> {
        // Get Data from API
        return flow {
            try {
                val weatherResponse = weatherService.getWeather(lat = lat, lon = lon)
                if (weatherResponse.isSuccessful) {
                    weatherResponse.body()?.let { weatherDto ->
                        weatherDao.deleteAll()
                        weatherDao.insertAll(weatherDto.toEntity())
                    }
                } else {
                    val errorResponse = Gson().fromJson(
                        weatherResponse.errorBody()?.charStream(),
                        ErrorDto::class.java
                    )
                    emit(Result.failure<WeatherEntity?>(Exception(errorResponse.message)))
                }
                emit(Result.success(weatherDao.getWeather()))
            } catch (e: Exception) {
                emit(Result.failure(e))
                emit(Result.success(weatherDao.getWeather()))
            }
        }
    }

    suspend fun getPlaces(query: String): Flow<Result<List<PlacesEntity>>> {
        return flow {
            try {
                val placesResponse = weatherService.getPossiblePlaces(
                    query = query
                )
                if (placesResponse.isSuccessful) {
                    emit(Result.success(placesResponse.body()!!.map { it.toEntity() }))
                } else {
                    val errorResponse =
                        Gson().fromJson(
                            placesResponse.errorBody()?.charStream(),
                            ErrorDto::class.java
                        )
                    emit(Result.failure<List<PlacesEntity>>(Exception(errorResponse.message)))
                }
            } catch (e: Exception) {
                emit(Result.failure<List<PlacesEntity>>(e))
            }
        }
    }
}