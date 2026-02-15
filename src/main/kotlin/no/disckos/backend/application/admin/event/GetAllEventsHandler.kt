package no.disckos.backend.application.admin.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetAllEventsHandler(
    private val eventRepository: EventRepository
) {
    @Transactional(readOnly = true)
    fun handle(): List<EventEntity> {

        return eventRepository.findAll(Sort.by(Sort.Direction.DESC, "date"))
    }
}
