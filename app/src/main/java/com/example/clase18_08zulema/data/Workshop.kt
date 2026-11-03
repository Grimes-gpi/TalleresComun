package com.example.clase18_08zulema.data

data class Workshop(
    val id: Long,
    val name: String,
    val description: String,
    val instructor: String,
    val category: String?,
    val imageUrl: String?,
    val maxCapacity: Int,
    val dateTime: String,
    val status: String,
    val registeredPeople: Long,
    val availableSlots: Long
)

data class WorkshopRequest(
    val name: String,
    val description: String,
    val instructor: String,
    val category: String,
    val imageUrl: String?,
    val maxCapacity: Int,
    val dateTime: String
)

data class Registration(
    val id: Long,
    val workshopId: Long,
    val workshopName: String,
    val beneficiaryName: String,
    val beneficiaryDocument: String,
    val registrationDate: String,
    val status: String
)

data class RegistrationRequest(
    val workshopId: Long,
    val beneficiaryName: String,
    val beneficiaryDocument: String
)

data class ApiError(val message: String? = null)
