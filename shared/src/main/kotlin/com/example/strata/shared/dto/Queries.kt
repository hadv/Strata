package com.example.strata.shared.dto

import java.math.BigInteger
import java.time.Instant
import java.util.UUID

data class AccountSummaryResponse(
    val accountId: UUID,
    val accountName: String,
    val balance: BigInteger,
    val lastUpdated: Instant,
    val eventVersion: Long
)
