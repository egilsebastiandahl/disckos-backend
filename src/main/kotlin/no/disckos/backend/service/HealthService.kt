package no.disckos.backend.service

import org.springframework.stereotype.Service

@Service
class HealthService {
    fun status(): String = "UP"
}