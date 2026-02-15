package no.disckos.backend.api.dto.admin.location

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateLocationRequest(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val description: String,

    @field:NotNull
    val lat: Double,

    @field:NotNull
    val lon: Double
)
