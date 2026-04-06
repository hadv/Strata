package com.example.strata.shared.routing

/**
 * Shared constants for Kafka topic names used by the Camel K smart router.
 * These topics are populated by the router module's content-based routing logic.
 */
object RoutingConstants {

    // --- Downstream Kafka Topics ---

    /** Topic for account lifecycle events (AccountCreated) */
    const val TOPIC_ACCOUNT_LIFECYCLE = "account-lifecycle"

    /** Topic for processed financial transactions (MoneyDeposited, MoneyWithdrawn) */
    const val TOPIC_TRANSACTION_PROCESSED = "transaction-processed"

    /** Topic for high-value transaction alerts (potential fraud) */
    const val TOPIC_HIGH_VALUE_ALERTS = "high-value-alerts"

    /** Dead-letter topic for unparseable / unknown events */
    const val TOPIC_DEAD_LETTER = "dead-letter-events"

    // --- Routing Thresholds ---

    /** Default threshold (in minor currency units) above which a transaction triggers a fraud alert */
    const val DEFAULT_HIGH_VALUE_THRESHOLD: Long = 50_000

    // --- Consumer Groups ---

    /** Consumer group for the smart router (separate from the query service group) */
    const val ROUTER_CONSUMER_GROUP = "strata-router-group"
}
