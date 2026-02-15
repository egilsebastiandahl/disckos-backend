package no.disckos.backend.application.admin.location

data class CreateLocationInput(
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double
)
