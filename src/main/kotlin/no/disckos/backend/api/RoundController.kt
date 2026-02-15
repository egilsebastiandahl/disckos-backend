package no.disckos.backend.api

import no.disckos.backend.api.dto.round.RoundResponse
import no.disckos.backend.application.admin.round.GetRoundHandler
import no.disckos.backend.application.admin.round.GetRoundsByEventHandler
import no.disckos.backend.application.round.GetRoundByEventOrderHandler
import no.disckos.backend.application.round.GetRoundsByEventOrderHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/round")
class RoundController(
    private val getRoundsByEventHandler: GetRoundsByEventHandler,
    private val getRoundHandler: GetRoundHandler,
    private val getRoundByEventOrderHandler: GetRoundByEventOrderHandler,
    private val getRoundsByEventOrderHandler: GetRoundsByEventOrderHandler
) {
    @GetMapping
    fun getByEvent(@RequestParam eventId: UUID): List<RoundResponse> =
        getRoundsByEventHandler.handle(eventId).map { it.toResponse() }

    @GetMapping("/event/{eventId}")
    fun getByEventOrdered(@PathVariable eventId: UUID): List<RoundResponse> =
        getRoundsByEventOrderHandler.handle(eventId).map { it.toResponse() }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): RoundResponse =
        getRoundHandler.handle(id).toResponse()

    @GetMapping("/event/{eventId}/round/{roundNumber}")
    fun getByEventAndRoundNumber(
        @PathVariable eventId: UUID,
        @PathVariable roundNumber: Int
    ): RoundResponse =
        getRoundByEventOrderHandler.handle(eventId, roundNumber).toResponse()
}
