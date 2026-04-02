package com.example.strata.query.model

import jakarta.persistence.*
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "account_summaries")
class AccountSummary(
    @Id
    @Column(name = "account_id")
    val accountId: UUID = UUID.randomUUID(),

    @Column(name = "account_name", nullable = false)
    var accountName: String = "",

    @Column(name = "balance", nullable = false)
    var balance: BigInteger = BigInteger.ZERO,

    @Column(name = "last_updated", nullable = false)
    var lastUpdated: Instant = Instant.now(),

    @Column(name = "event_version", nullable = false)
    var eventVersion: Long = 0
)
