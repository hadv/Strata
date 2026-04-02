package com.example.strata.shared.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AccountCreated::class, name = "AccountCreated"),
    JsonSubTypes.Type(value = MoneyDeposited::class, name = "MoneyDeposited"),
    JsonSubTypes.Type(value = MoneyWithdrawn::class, name = "MoneyWithdrawn")
)
sealed interface DomainEvent {
    val eventId: UUID
    val aggregateId: UUID
    val occurredAt: Instant
}

data class AccountCreated(
    override val eventId: UUID = UUID.randomUUID(),
    override val aggregateId: UUID,
    override val occurredAt: Instant = Instant.now(),
    val accountName: String,
    val initialBalance: BigInteger
) : DomainEvent

data class MoneyDeposited(
    override val eventId: UUID = UUID.randomUUID(),
    override val aggregateId: UUID,
    override val occurredAt: Instant = Instant.now(),
    val amount: BigInteger,
    val description: String = ""
) : DomainEvent

data class MoneyWithdrawn(
    override val eventId: UUID = UUID.randomUUID(),
    override val aggregateId: UUID,
    override val occurredAt: Instant = Instant.now(),
    val amount: BigInteger,
    val description: String = ""
) : DomainEvent
