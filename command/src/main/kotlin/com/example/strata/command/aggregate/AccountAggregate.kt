package com.example.strata.command.aggregate

import com.example.strata.shared.dto.CreateAccountCommand
import com.example.strata.shared.dto.DepositMoneyCommand
import com.example.strata.shared.dto.WithdrawMoneyCommand
import com.example.strata.shared.events.*
import java.math.BigDecimal
import java.util.UUID

class AccountAggregate {
    var id: UUID? = null
        private set
    var name: String = ""
        private set
    var balance: BigDecimal = BigDecimal.ZERO
        private set
    var version: Long = 0
        private set
    var created: Boolean = false
        private set

    // --- Command Handlers (produce events) ---

    fun create(command: CreateAccountCommand): List<DomainEvent> {
        require(!created) { "Account ${command.accountId} already exists" }
        require(command.accountName.isNotBlank()) { "Account name must not be blank" }
        require(command.initialBalance >= BigDecimal.ZERO) { "Initial balance must not be negative" }

        return listOf(
            AccountCreated(
                aggregateId = command.accountId,
                accountName = command.accountName,
                initialBalance = command.initialBalance
            )
        )
    }

    fun deposit(command: DepositMoneyCommand): List<DomainEvent> {
        require(created) { "Account ${command.accountId} does not exist" }
        require(command.amount > BigDecimal.ZERO) { "Deposit amount must be positive" }

        return listOf(
            MoneyDeposited(
                aggregateId = command.accountId,
                amount = command.amount,
                description = command.description
            )
        )
    }

    fun withdraw(command: WithdrawMoneyCommand): List<DomainEvent> {
        require(created) { "Account ${command.accountId} does not exist" }
        require(command.amount > BigDecimal.ZERO) { "Withdrawal amount must be positive" }
        require(balance >= command.amount) {
            "Insufficient funds: balance=$balance, withdrawal=${command.amount}"
        }

        return listOf(
            MoneyWithdrawn(
                aggregateId = command.accountId,
                amount = command.amount,
                description = command.description
            )
        )
    }

    // --- Event Applicators (mutate state) ---

    fun apply(event: DomainEvent) {
        when (event) {
            is AccountCreated -> {
                id = event.aggregateId
                name = event.accountName
                balance = event.initialBalance
                created = true
            }
            is MoneyDeposited -> {
                balance = balance.add(event.amount)
            }
            is MoneyWithdrawn -> {
                balance = balance.subtract(event.amount)
            }
        }
        version++
    }

    fun applyAll(events: List<DomainEvent>) {
        events.forEach { apply(it) }
    }

    companion object {
        fun fromHistory(events: List<DomainEvent>): AccountAggregate {
            return AccountAggregate().also { it.applyAll(events) }
        }
    }
}
