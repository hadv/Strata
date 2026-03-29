package com.example.strata.query.projection

import com.example.strata.query.model.AccountSummary
import com.example.strata.query.repository.AccountSummaryRepository
import com.example.strata.shared.events.AccountCreated
import com.example.strata.shared.events.DomainEvent
import com.example.strata.shared.events.MoneyDeposited
import com.example.strata.shared.events.MoneyWithdrawn
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class AccountProjection(
    private val repository: AccountSummaryRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AccountProjection::class.java)

    /**
     * Consumes CDC events from Debezium.
     * The Debezium envelope wraps the actual event data in a standardized format:
     * {
     *   "payload": {
     *     "before": null,
     *     "after": {
     *       "event_id": "...",
     *       "aggregate_id": "...",
     *       "event_type": "AccountCreated",
     *       "event_data": "{...json...}",
     *       "version": 1,
     *       ...
     *     },
     *     "op": "c"  // c=create, u=update, d=delete
     *   }
     * }
     */
    @KafkaListener(
        topics = ["\${app.kafka.topic:postgres.public.events}"],
        groupId = "\${app.kafka.group-id:strata-query-group}"
    )
    @Transactional
    fun onEvent(message: String) {
        try {
            val root = objectMapper.readTree(message)
            val payload = root.get("payload") ?: root // handle both envelope and raw

            // Only process INSERT operations (new events)
            val operation = payload.get("op")?.asText()
            if (operation != null && operation != "c" && operation != "r") {
                log.debug("Skipping non-insert operation: {}", operation)
                return
            }

            val after = payload.get("after") ?: payload
            val eventType = after.get("event_type")?.asText() ?: return
            val eventDataJson = after.get("event_data")?.asText() ?: return
            val aggregateId = after.get("aggregate_id")?.asText() ?: return
            val version = after.get("version")?.asLong() ?: return

            log.info("Processing CDC event: type={}, aggregateId={}, version={}",
                eventType, aggregateId, version)

            val event = objectMapper.readValue(eventDataJson, DomainEvent::class.java)

            // Idempotency check: skip if already processed this version
            val uuid = UUID.fromString(aggregateId)
            val existing = repository.findById(uuid).orElse(null)
            if (existing != null && existing.eventVersion >= version) {
                log.debug("Skipping already-processed event: version={}", version)
                return
            }

            applyEvent(event, version)

        } catch (ex: Exception) {
            log.error("Error processing CDC event", ex)
            throw ex // re-throw to trigger Kafka retry
        }
    }

    private fun applyEvent(event: DomainEvent, version: Long) {
        when (event) {
            is AccountCreated -> {
                val summary = AccountSummary(
                    accountId = event.aggregateId,
                    accountName = event.accountName,
                    balance = event.initialBalance,
                    lastUpdated = event.occurredAt,
                    eventVersion = version
                )
                repository.save(summary)
                log.info("Projected AccountCreated: id={}, name={}, balance={}",
                    event.aggregateId, event.accountName, event.initialBalance)
            }

            is MoneyDeposited -> {
                val summary = repository.findById(event.aggregateId).orElseThrow {
                    IllegalStateException("Account ${event.aggregateId} not found for projection")
                }
                summary.balance = summary.balance.add(event.amount)
                summary.lastUpdated = event.occurredAt
                summary.eventVersion = version
                repository.save(summary)
                log.info("Projected MoneyDeposited: id={}, amount={}, newBalance={}",
                    event.aggregateId, event.amount, summary.balance)
            }

            is MoneyWithdrawn -> {
                val summary = repository.findById(event.aggregateId).orElseThrow {
                    IllegalStateException("Account ${event.aggregateId} not found for projection")
                }
                summary.balance = summary.balance.subtract(event.amount)
                summary.lastUpdated = event.occurredAt
                summary.eventVersion = version
                repository.save(summary)
                log.info("Projected MoneyWithdrawn: id={}, amount={}, newBalance={}",
                    event.aggregateId, event.amount, summary.balance)
            }
        }
    }
}
