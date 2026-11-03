package no.disckos.backend.repository

import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscImportedCardEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UdiscPlayerAliasRepository : JpaRepository<UdiscPlayerAliasEntity, UUID> {
    fun findByUdiscName(udiscName: String): UdiscPlayerAliasEntity?
}

interface UdiscCourseAliasRepository : JpaRepository<UdiscCourseAliasEntity, UUID> {
    fun findByUdiscCourse(udiscCourse: String): UdiscCourseAliasEntity?
}

interface UdiscImportedCardRepository : JpaRepository<UdiscImportedCardEntity, String>
