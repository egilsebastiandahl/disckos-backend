package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.location.AdminLocationResponse
import no.disckos.backend.api.dto.admin.location.CreateLocationRequest
import no.disckos.backend.api.dto.admin.location.UpdateLocationRequest
import no.disckos.backend.application.admin.location.CreateLocationInput
import no.disckos.backend.application.admin.location.UpdateLocationInput
import no.disckos.backend.domain.LocationEntity

fun CreateLocationRequest.toInput(): CreateLocationInput =
    CreateLocationInput(
        name = name,
        description = description,
        lat = lat,
        lon = lon
    )

fun UpdateLocationRequest.toInput(): UpdateLocationInput =
    UpdateLocationInput(
        name = name,
        description = description,
        lat = lat,
        lon = lon
    )

fun LocationEntity.toAdminResponse(): AdminLocationResponse =
    AdminLocationResponse(
        id = id,
        name = name,
        description = description,
        lat = lat,
        lon = lon
    )
