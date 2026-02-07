package no.disckos.backend.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminProtectedController {

    @GetMapping("/players")
    fun getPlayers(): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(emptyList())
    }

    @PostMapping("/players")
    fun createPlayer(@RequestBody payload: Map<String, Any?>): ResponseEntity<Map<String, Any?>> {
        // TODO: replace with DB-backed create logic once you add a players table.
        return ResponseEntity.status(HttpStatus.CREATED).body(payload)
    }

    @GetMapping("/events")
    fun getEvents(): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(emptyList())
    }

    @PostMapping("/events")
    fun createEvent(@RequestBody payload: Map<String, Any?>): ResponseEntity<Map<String, Any?>> {
        // TODO: replace with DB-backed create logic and refresh-token storage when multi-admin is added.
        return ResponseEntity.status(HttpStatus.CREATED).body(payload)
    }
}
