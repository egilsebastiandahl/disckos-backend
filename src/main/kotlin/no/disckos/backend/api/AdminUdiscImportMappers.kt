package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.udiscimport.UdiscCardResultResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscCommitRequest
import no.disckos.backend.api.dto.admin.udiscimport.UdiscEventOptionResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscImportedRoundResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscPreviewCardResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscPreviewPlayerResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscPreviewResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscRoundOptionResponse
import no.disckos.backend.application.admin.udiscimport.UdiscCardResult
import no.disckos.backend.application.admin.udiscimport.UdiscCommitCard
import no.disckos.backend.application.admin.udiscimport.UdiscCommitInput
import no.disckos.backend.application.admin.udiscimport.UdiscCommitPlayer
import no.disckos.backend.application.admin.udiscimport.UdiscEventOption
import no.disckos.backend.application.admin.udiscimport.UdiscPreview
import no.disckos.backend.application.admin.udiscimport.UdiscPreviewCard

fun UdiscCommitRequest.toInput(): UdiscCommitInput =
    UdiscCommitInput(
        cards = cards.map { card ->
            UdiscCommitCard(
                key = card.key,
                eventId = card.eventId,
                roundOrdinal = card.roundOrdinal,
                locationId = card.locationId,
                newLocationName = card.newLocationName,
                startedAt = card.startedAt,
                courseName = card.courseName,
                pars = card.pars,
                players = card.players.map { p ->
                    UdiscCommitPlayer(
                        udiscName = p.udiscName,
                        playerId = p.playerId,
                        newPlayerName = p.newPlayerName,
                        throws = p.throws,
                    )
                },
            )
        },
    )

fun UdiscPreview.toResponse(): UdiscPreviewResponse =
    UdiscPreviewResponse(cardsInFile = cardsInFile, cards = cards.map { it.toResponse() })

private fun UdiscPreviewCard.toResponse() =
    UdiscPreviewCardResponse(
        key = key,
        courseName = courseName,
        layoutName = layoutName,
        startedAt = startedAt,
        pars = pars,
        players = players.map { p ->
            UdiscPreviewPlayerResponse(
                udiscName = p.udiscName,
                throws = p.throws,
                suggestedPlayerId = p.suggestedPlayerId,
                match = p.match.name.lowercase(),
            )
        },
        eventOptions = eventOptions.map { it.toResponse() },
        suggestedEventId = suggestedEventId,
        suggestedRoundOrdinal = suggestedRoundOrdinal,
        suggestedLocationId = suggestedLocationId,
        courseMatch = courseMatch.name.lowercase(),
        importedRound = importedRound?.let {
            UdiscImportedRoundResponse(roundId = it.roundId, eventId = it.eventId, playerIds = it.playerIds.toList())
        },
        selected = selected,
        warnings = warnings,
    )

private fun UdiscEventOption.toResponse() =
    UdiscEventOptionResponse(
        id = id,
        title = title,
        date = date,
        locationName = locationName,
        locationMatch = locationMatch.name.lowercase(),
        teamEvent = teamEvent,
        plannedRounds = plannedRounds,
        rounds = rounds.map { r ->
            UdiscRoundOptionResponse(ordinal = r.ordinal, roundId = r.roundId, pars = r.pars, playerIds = r.playerIds.toList())
        },
    )

fun UdiscCardResult.toResponse() =
    UdiscCardResultResponse(
        key = key,
        eventId = eventId,
        roundId = roundId,
        roundOrdinal = roundOrdinal,
        createdRound = createdRound,
        importedPlayers = importedPlayers,
        skippedPlayers = skippedPlayers,
        error = error,
    )
