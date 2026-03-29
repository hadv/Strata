package com.example.strata.command.handler

import com.example.strata.command.aggregate.AccountAggregate
import com.example.strata.command.store.EventStore
import com.example.strata.shared.dto.CreateAccountCommand
import com.example.strata.shared.dto.DepositMoneyCommand
import com.example.strata.shared.dto.WithdrawMoneyCommand
import com.example.strata.shared.events.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AccountCommandHandler(
    private val eventStore: EventStore
) {
    private val log = LoggerFactory.getLogger(AccountCommandHandler::class.java)

    fun handle(command: CreateAccountCommand): UUID {
        log.info("Handling CreateAccountCommand: accountId={}, name={}",
            command.accountId, command.accountName)

        val aggregate = AccountAggregate()
        val events = aggregate.create(command)
        eventStore.saveEvents(command.accountId, events, expectedVersion = 0)

        log.info("Account created successfully: {}", command.accountId)
        return command.accountId
    }

    fun handle(command: DepositMoneyCommand) {
        log.info("Handling DepositMoneyCommand: accountId={}, amount={}",
            command.accountId, command.amount)

        val (aggregate, version) = loadAggregate(command.accountId)
        val events = aggregate.deposit(command)
        eventStore.saveEvents(command.accountId, events, expectedVersion = version)

        log.info("Deposit successful: accountId={}, amount={}", command.accountId, command.amount)
    }

    fun handle(command: WithdrawMoneyCommand) {
        log.info("Handling WithdrawMoneyCommand: accountId={}, amount={}",
            command.accountId, command.amount)

        val (aggregate, version) = loadAggregate(command.accountId)
        val events = aggregate.withdraw(command)
        eventStore.saveEvents(command.accountId, events, expectedVersion = version)

        log.info("Withdrawal successful: accountId={}, amount={}", command.accountId, command.amount)
    }

    private fun loadAggregate(aggregateId: UUID): Pair<AccountAggregate, Long> {
        val events: List<DomainEvent> = eventStore.getEvents(aggregateId)
        require(events.isNotEmpty()) { "Account $aggregateId not found" }
        val aggregate = AccountAggregate.fromHistory(events)
        val version = eventStore.getCurrentVersion(aggregateId)
        return Pair(aggregate, version)
    }
}
