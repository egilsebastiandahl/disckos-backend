package no.disckos.backend.application.admin.event

import java.time.OffsetDateTime

data class CreateEventInput(
    val date: OffsetDateTime,
    val title: String,
    val description: String,
    val locationId: java.util.UUID,
    val teamEvent: Boolean,
    val published: Boolean,
    val rounds: Int
)
