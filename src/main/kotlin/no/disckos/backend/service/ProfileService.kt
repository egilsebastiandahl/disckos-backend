package no.disckos.backend.service

import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.ProfileRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val playerRepository: PlayerRepository,
) {

    fun getOrCreateProfile(userId: UUID, email: String?): Profile {
        val existing = profileRepository.findById(userId)
        if (existing.isPresent) return existing.get()

        val p = Profile()
        p.id = userId
        if (!email.isNullOrBlank()) {
            val localPart = email.substringBefore("@")
            p.username = localPart
            p.displayName = localPart
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
        p.updatedAt = OffsetDateTime.now()
        return profileRepository.save(p)
    }

    /**
     * Gives or revokes the admin role. Who may call this is checked by the caller (owners only);
     * this enforces the rule that holds for everyone: an owner is always an admin.
     */
    fun setAdmin(profileId: UUID, admin: Boolean): Profile {
        val profile = profileRepository.findById(profileId)
            .orElseThrow { NoSuchElementException("Profile not found") }
        if (!admin && profile.isOwner) {
            throw IllegalStateException("An owner is always an admin")
        }
        if (profile.isAdmin == admin) return profile

        profile.isAdmin = admin
        profile.updatedAt = OffsetDateTime.now()
        return profileRepository.save(profile)
    }

    fun linkPlayer(userId: UUID, playerId: UUID?): Profile {
        val profile = profileRepository.findById(userId)
            .orElseThrow { NoSuchElementException("Profile not found") }

        if (playerId == null) {
            // Unlink
            profile.player = null
        } else {
            // Check the player exists
            val player = playerRepository.findById(playerId)
                .orElseThrow { NoSuchElementException("Player not found") }

            // Check no other profile is already linked to this player
            val existing = profileRepository.findByPlayerId(playerId)
            if (existing != null && existing.id != userId) {
                throw IllegalStateException("This player is already linked to another profile")
            }

            profile.player = player
        }

        profile.updatedAt = OffsetDateTime.now()
        return profileRepository.save(profile)
    }
}
