package no.disckos.backend.application.admin.event

import java.time.OffsetDateTime

data class CreateEventInput(
    val date: OffsetDateTime,
    val title: String,
    val description: String,
    val location: String,
    val teamEvent: Boolean,
    val rounds: Int
)