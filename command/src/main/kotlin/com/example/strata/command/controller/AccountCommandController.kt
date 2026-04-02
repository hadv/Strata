package com.example.strata.command.controller

import com.example.strata.command.handler.AccountCommandHandler
import com.example.strata.command.store.OptimisticConcurrencyException
import com.example.strata.shared.dto.CreateAccountCommand
import com.example.strata.shared.dto.DepositMoneyCommand
import com.example.strata.shared.dto.WithdrawMoneyCommand
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/accounts")
class AccountCommandController(
    private val commandHandler: AccountCommandHandler
) {

    @PostMapping
    fun createAccount(@RequestBody command: CreateAccountCommand): ResponseEntity<Map<String, Any>> {
        val accountId = commandHandler.handle(command)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapOf("accountId" to accountId, "message" to "Account created successfully"))
    }

    @PostMapping("/{accountId}/deposit")
    fun deposit(
        @PathVariable accountId: UUID,
        @RequestBody request: DepositRequest
    ): ResponseEntity<Map<String, String>> {
        commandHandler.handle(
            DepositMoneyCommand(
                accountId = accountId,
                amount = request.amount,
                description = request.description
            )
        )
        return ResponseEntity.ok(mapOf("message" to "Deposit successful"))
    }

    @PostMapping("/{accountId}/withdraw")
    fun withdraw(
        @PathVariable accountId: UUID,
        @RequestBody request: WithdrawRequest
    ): ResponseEntity<Map<String, String>> {
        commandHandler.handle(
            WithdrawMoneyCommand(
                accountId = accountId,
                amount = request.amount,
                description = request.description
            )
        )
        return ResponseEntity.ok(mapOf("message" to "Withdrawal successful"))
    }

    // --- Exception Handlers ---

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to (ex.message ?: "Bad request")))
    }

    @ExceptionHandler(OptimisticConcurrencyException::class)
    fun handleConcurrency(ex: OptimisticConcurrencyException): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("error" to (ex.message ?: "Concurrency conflict")))
    }
}

data class DepositRequest(
    val amount: java.math.BigInteger,
    val description: String = ""
)

data class WithdrawRequest(
    val amount: java.math.BigInteger,
    val description: String = ""
)
