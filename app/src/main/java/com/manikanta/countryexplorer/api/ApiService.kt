package com.manikanta.countryexplorer.api

import com.manikanta.countryexplorer.model.CountryDto
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {

    @GET("countries.json")
    suspend fun getAllCountries(): Response<List<CountryDto>>
}

object RetrofitClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/mledoze/countries/master/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
