package com.example.strata.command.store

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventStoreRepository : JpaRepository<EventStoreEntity, UUID> {
    fun findByAggregateIdOrderByVersionAsc(aggregateId: UUID): List<EventStoreEntity>
    fun countByAggregateId(aggregateId: UUID): Long
}
