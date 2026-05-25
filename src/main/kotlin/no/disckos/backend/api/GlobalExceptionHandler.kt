package no.disckos.backend.api

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<Map<String, Any?>> =
        ResponseEntity
            .status(ex.statusCode)
            .body(
                mapOf(
                    "timestamp" to OffsetDateTime.now().toString(),
                    "status" to ex.statusCode.value(),
                    "error" to ex.statusCode.toString(),
                    "message" to ex.reason,
                ),
            )

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleIllegalArgument(ex: RuntimeException): ResponseEntity<Map<String, Any?>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                mapOf(
                    "timestamp" to OffsetDateTime.now().toString(),
                    "status" to 400,
                    "error" to "Bad Request",
                    "message" to (ex.message ?: ex.javaClass.simpleName),
                ),
            )

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccess(ex: DataAccessException): ResponseEntity<Map<String, Any?>> {
        log.error("Data access error", ex)
        val rootMessage = ex.mostSpecificCause?.message ?: ex.message ?: ex.javaClass.simpleName
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                mapOf(
                    "timestamp" to OffsetDateTime.now().toString(),
                    "status" to 500,
                    "error" to "Database Error",
                    "message" to rootMessage,
                    "type" to ex.javaClass.simpleName,
                ),
            )
    }

    // Catch-all: log full stack trace server-side, return the exception message to the client
    // so the frontend can show something more useful than "Internal Server Error".
    @ExceptionHandler(Exception::class)
    fun handleAnyException(ex: Exception): ResponseEntity<Map<String, Any?>> {
        log.error("Unhandled exception", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                mapOf(
                    "timestamp" to OffsetDateTime.now().toString(),
                    "status" to 500,
                    "error" to "Internal Server Error",
                    "message" to (ex.message ?: ex.javaClass.simpleName),
                    "type" to ex.javaClass.simpleName,
                ),
            )
    }
}
