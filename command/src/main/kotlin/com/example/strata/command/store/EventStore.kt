package com.example.strata.command.store

import com.example.strata.shared.events.DomainEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class EventStore(
    private val repository: EventStoreRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(EventStore::class.java)

    @Transactional
    fun saveEvents(aggregateId: UUID, events: List<DomainEvent>, expectedVersion: Long) {
        try {
            events.forEachIndexed { index, event ->
                val entity = EventStoreEntity(
                    eventId = event.eventId,
                    aggregateId = aggregateId,
                    aggregateType = "Account",
                    eventType = event::class.simpleName ?: "Unknown",
                    eventData = objectMapper.writeValueAsString(event),
                    version = expectedVersion + index + 1,
                    createdAt = event.occurredAt
                )
                repository.save(entity)
                log.info("Saved event: type={}, aggregateId={}, version={}",
                    entity.eventType, aggregateId, entity.version)
            }
        } catch (ex: DataIntegrityViolationException) {
            throw OptimisticConcurrencyException(
                "Concurrency conflict for aggregate $aggregateId at version $expectedVersion"
            )
        }
    }

    @Transactional(readOnly = true)
    fun getEvents(aggregateId: UUID): List<DomainEvent> {
        return repository.findByAggregateIdOrderByVersionAsc(aggregateId)
            .map { entity ->
                objectMapper.readValue(entity.eventData, DomainEvent::class.java)
            }
    }

    @Transactional(readOnly = true)
    fun getCurrentVersion(aggregateId: UUID): Long {
        return repository.countByAggregateId(aggregateId)
    }
}

class OptimisticConcurrencyException(message: String) : RuntimeException(message)
