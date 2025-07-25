package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConnectionJpaRepository : JpaRepository<ConnectionEntity, String> {
    fun findByName(name: String): ConnectionEntity?
}