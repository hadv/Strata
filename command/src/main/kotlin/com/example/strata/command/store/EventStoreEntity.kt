package com.example.strata.command.store

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "events",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["aggregate_id", "version"])
    ]
)
class EventStoreEntity(
    @Id
    @Column(name = "event_id")
    val eventId: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String = "",

    @Column(name = "event_type", nullable = false)
    val eventType: String = "",

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    val eventData: String = "",

    @Column(name = "version", nullable = false)
    val version: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
