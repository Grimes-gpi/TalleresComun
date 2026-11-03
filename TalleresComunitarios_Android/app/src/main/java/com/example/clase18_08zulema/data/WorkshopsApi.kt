package com.example.clase18_08zulema.data

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WorkshopsApi {
    @GET("api/workshops") fun listWorkshops(
        @Query("name") name: String? = null,
        @Query("category") category: String? = null
    ): Call<List<Workshop>>
    @GET("api/workshops/{id}") fun getWorkshop(@Path("id") id: Long): Call<Workshop>
    @POST("api/workshops") fun createWorkshop(@Body request: WorkshopRequest): Call<Workshop>
    @PUT("api/workshops/{id}") fun updateWorkshop(@Path("id") id: Long, @Body request: WorkshopRequest): Call<Workshop>
    @DELETE("api/workshops/{id}") fun cancelWorkshop(@Path("id") id: Long): Call<Void>
    @GET("api/workshops/{id}/registrations") fun listRegistrations(@Path("id") id: Long): Call<List<Registration>>
    @POST("api/registrations") fun createRegistration(@Body request: RegistrationRequest): Call<Registration>

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8081/"

        @Volatile
        var baseUrl: String = DEFAULT_BASE_URL
            private set

        @Volatile
        private var cachedInstance: WorkshopsApi? = null

        val instance: WorkshopsApi
            get() = cachedInstance ?: synchronized(this) {
                cachedInstance ?: create(baseUrl).also { cachedInstance = it }
            }

        fun configure(url: String) = synchronized(this) {
            if (url != baseUrl || cachedInstance == null) {
                baseUrl = url
                cachedInstance = create(url)
            }
        }

        private fun create(url: String): WorkshopsApi = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WorkshopsApi::class.java)
    }
}
