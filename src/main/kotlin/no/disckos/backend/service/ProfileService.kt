package no.disckos.backend.service

import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.ProfileRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ProfileService(private val profileRepository: ProfileRepository) {

    fun getOrCreateProfile(userId: UUID, email: String?): Profile {
        val existing = profileRepository.findById(userId)
        if (existing.isPresent) return existing.get()

        val p = Profile()
        p.id = userId
        if (!email.isNullOrBlank()) {
            p.username = email.substringBefore("@")
            p.displayName = email
        }
        p.createdAt = OffsetDateTime.now()
        p.updatedAt = OffsetDateTime.now()
        return profileRepository.save(p)
    }

    fun updateProfile(userId: UUID, username: String?, displayName: String?, bio: String?): Profile {
        val existing = profileRepository.findById(userId)
        if (!existing.isPresent) throw NoSuchElementException("Profile not found")
        val p = existing.get()
        if (username != null) p.username = username
        if (displayName != null) p.displayName = displayName
        if (bio != null) p.bio = bio
        p.updatedAt = java.time.OffsetDateTime.now()
        return profileRepository.save(p)
    }
}
