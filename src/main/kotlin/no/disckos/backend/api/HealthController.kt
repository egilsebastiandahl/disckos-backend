package no.disckos.backend.api

import no.disckos.backend.api.dto.health.HealthResponse
import no.disckos.backend.service.HealthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController(
    private val healthService: HealthService
) {
    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse(status = healthService.status())
}