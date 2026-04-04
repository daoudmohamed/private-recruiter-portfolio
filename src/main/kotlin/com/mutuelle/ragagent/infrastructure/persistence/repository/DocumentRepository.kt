package com.mutuelle.ragagent.infrastructure.persistence.repository

import com.mutuelle.ragagent.domain.model.Document
import com.mutuelle.ragagent.domain.model.DocumentType
import com.mutuelle.ragagent.infrastructure.persistence.entity.DocumentEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * R2DBC repository for documents.
 */
@Repository
interface DocumentR2dbcRepository : CoroutineCrudRepository<DocumentEntity, UUID> {

    @Query("""
        SELECT * FROM documents
        WHERE adherent_id = :adherentId
        AND (valid_until IS NULL OR valid_until >= CURRENT_DATE)
        ORDER BY generated_at DESC
    """)
    suspend fun findValidByAdherentId(adherentId: UUID): List<DocumentEntity>

    @Query("""
        SELECT * FROM documents
        WHERE adherent_id = :adherentId
        AND type = :type
        AND (valid_until IS NULL OR valid_until >= CURRENT_DATE)
        ORDER BY generated_at DESC
        LIMIT 1
    """)
    suspend fun findLatestByAdherentIdAndType(adherentId: UUID, type: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE adherent_id = :adherentId ORDER BY generated_at DESC")
    suspend fun findByAdherentId(adherentId: UUID): List<DocumentEntity>
}

/**
 * High-level repository for documents.
 */
@Repository
class DocumentRepository(
    private val documentR2dbcRepository: DocumentR2dbcRepository
) {
    suspend fun findValidByAdherentId(adherentId: UUID): List<Document> {
        return documentR2dbcRepository.findValidByAdherentId(adherentId)
            .map { it.toDomain() }
    }

    suspend fun findLatestByAdherentIdAndType(adherentId: UUID, type: DocumentType): Document? {
        return documentR2dbcRepository.findLatestByAdherentIdAndType(adherentId, type.name)
            ?.toDomain()
    }

    suspend fun findById(documentId: UUID): Document? {
        return documentR2dbcRepository.findById(documentId)?.toDomain()
    }

    suspend fun findByAdherentId(adherentId: UUID): List<Document> {
        return documentR2dbcRepository.findByAdherentId(adherentId)
            .map { it.toDomain() }
    }

    private fun DocumentEntity.toDomain(): Document {
        return Document(
            id = this.id!!,
            adherentId = this.adherentId,
            type = DocumentType.valueOf(this.type),
            title = this.title,
            filename = this.filename,
            contentType = this.contentType,
            filePath = this.filePath,
            fileSize = this.fileSize,
            generatedAt = this.generatedAt,
            validFrom = this.validFrom,
            validUntil = this.validUntil
        )
    }
}
