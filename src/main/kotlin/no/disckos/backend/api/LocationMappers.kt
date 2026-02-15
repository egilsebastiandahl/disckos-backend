package no.disckos.backend.api

import no.disckos.backend.api.dto.location.LocationResponse
import no.disckos.backend.domain.LocationEntity

fun LocationEntity.toResponse(): LocationResponse =
    LocationResponse(
        id = id,
        name = name,
        description = description,
        lat = lat,
        lon = lon
    )
