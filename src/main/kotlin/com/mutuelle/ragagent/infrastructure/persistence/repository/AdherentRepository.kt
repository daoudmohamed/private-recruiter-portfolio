package com.mutuelle.ragagent.infrastructure.persistence.repository

import com.mutuelle.ragagent.domain.model.Adherent
import com.mutuelle.ragagent.domain.model.Address
import com.mutuelle.ragagent.domain.model.Civilite
import com.mutuelle.ragagent.domain.model.ContactPreference
import com.mutuelle.ragagent.infrastructure.persistence.entity.AdherentEntity
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for adherent data access.
 */
@Repository
interface AdherentR2dbcRepository : CoroutineCrudRepository<AdherentEntity, UUID> {

    suspend fun findByNumeroAdherent(numeroAdherent: String): AdherentEntity?

    suspend fun findByEmail(email: String): AdherentEntity?

    @Query("SELECT * FROM adherents WHERE LOWER(last_name) LIKE LOWER(CONCAT('%', :name, '%'))")
    suspend fun findByLastNameContaining(name: String): List<AdherentEntity>
}

/**
 * High-level repository that maps entities to domain models.
 */
@Repository
class AdherentRepository(
    private val r2dbcRepository: AdherentR2dbcRepository
) {
    suspend fun findById(id: UUID): Adherent? {
        return r2dbcRepository.findById(id)?.toDomain()
    }

    suspend fun findByNumeroAdherent(numeroAdherent: String): Adherent? {
        return r2dbcRepository.findByNumeroAdherent(numeroAdherent)?.toDomain()
    }

    suspend fun findByEmail(email: String): Adherent? {
        return r2dbcRepository.findByEmail(email)?.toDomain()
    }

    suspend fun save(adherent: Adherent): Adherent {
        val entity = AdherentEntity.fromDomain(adherent)
        return r2dbcRepository.save(entity).toDomain()
    }

    private fun AdherentEntity.toDomain(): Adherent {
        return Adherent(
            id = this.id!!,
            numeroAdherent = this.numeroAdherent,
            civilite = Civilite.valueOf(this.civilite),
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            phone = this.phone,
            dateOfBirth = this.dateOfBirth,
            address = Address(
                street = this.street,
                additionalInfo = this.additionalInfo,
                postalCode = this.postalCode,
                city = this.city,
                country = this.country
            ),
            preferenceContact = ContactPreference.valueOf(this.preferenceContact),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
