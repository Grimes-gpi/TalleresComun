package com.example.clase18_08zulema.data

import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

object WorkshopsRepository {
    private val api get() = WorkshopsApi.instance
    private val gson = Gson()

    fun workshops(name: String? = null, category: String? = null, done: (ApiResult<List<Workshop>>) -> Unit) =
        api.listWorkshops(name?.takeIf { it.isNotBlank() }, category?.takeIf { it.isNotBlank() }).deliver(done)

    fun workshop(id: Long, done: (ApiResult<Workshop>) -> Unit) = api.getWorkshop(id).deliver(done)
    fun registrations(id: Long, done: (ApiResult<List<Registration>>) -> Unit) = api.listRegistrations(id).deliver(done)
    fun saveWorkshop(id: Long?, request: WorkshopRequest, done: (ApiResult<Workshop>) -> Unit) =
        (if (id == null) api.createWorkshop(request) else api.updateWorkshop(id, request)).deliver(done)
    fun cancelWorkshop(id: Long, done: (ApiResult<Boolean>) -> Unit) = api.cancelWorkshop(id).enqueue(object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if (response.isSuccessful) done(ApiResult.Success(true))
            else done(ApiResult.Failure("No se pudo cancelar el taller (${response.code()})."))
        }
        override fun onFailure(call: Call<Void>, error: Throwable) {
            done(ApiResult.Failure(connectionMessage(error)))
        }
    })
    fun register(request: RegistrationRequest, done: (ApiResult<Registration>) -> Unit) = api.createRegistration(request).deliver(done)

    private fun <T> Call<T>.deliver(done: (ApiResult<T>) -> Unit) = enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                response.body()?.let { done(ApiResult.Success(it)) }
                    ?: done(ApiResult.Failure("El servidor respondió sin datos."))
            } else {
                val message = runCatching {
                    gson.fromJson(response.errorBody()?.string(), ApiError::class.java)?.message
                }.getOrNull()
                done(ApiResult.Failure(message ?: when (response.code()) {
                    404 -> "No encontramos ese taller."
                    409 -> "No se pudo completar porque el taller está lleno o la persona ya está inscrita."
                    else -> "No se pudo completar la solicitud (${response.code()})."
                }))
            }
        }

        override fun onFailure(call: Call<T>, error: Throwable) {
            done(ApiResult.Failure(connectionMessage(error)))
        }
    })

    private fun connectionMessage(error: Throwable): String {
        val cause = error.localizedMessage?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
        return "No logramos conectar con la API. Dirección: ${WorkshopsApi.baseUrl}\nDetalle: $cause\nEn el emulador usa 10.0.2.2; en un teléfono configura la IP local del computador."
    }
}
