package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.event.AdminEventResponse
import no.disckos.backend.api.dto.admin.event.CreateEventRequest
import no.disckos.backend.api.dto.admin.event.UpdateEventRequest
import no.disckos.backend.application.admin.event.CreateEventInput
import no.disckos.backend.application.admin.event.UpdateEventInput
import no.disckos.backend.domain.EventEntity

fun CreateEventRequest.toInput(): CreateEventInput =
    CreateEventInput(
        date = date,
        title = title,
        description = description,
        location = location,
        teamEvent = teamEvent,
        published = published,
        rounds = rounds
    )

fun UpdateEventRequest.toInput(id: java.util.UUID): UpdateEventInput =
    UpdateEventInput(
        id = id,
        date = date,
        title = title,
        description = description,
        location = location,
        teamEvent = teamEvent,
        rounds = rounds
    )

fun EventEntity.toAdminResponse(): AdminEventResponse =
    AdminEventResponse(
        id = id,
        date = date,
        title = title,
        description = description,
        location = location,
        teamEvent = teamEvent,
        rounds = rounds,
        published = published
    )
