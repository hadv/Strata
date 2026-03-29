package com.example.strata.query.controller

import com.example.strata.query.repository.AccountSummaryRepository
import com.example.strata.shared.dto.AccountSummaryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/accounts")
class AccountQueryController(
    private val repository: AccountSummaryRepository
) {

    @GetMapping
    fun getAllAccounts(): ResponseEntity<List<AccountSummaryResponse>> {
        val accounts = repository.findAll().map { summary ->
            AccountSummaryResponse(
                accountId = summary.accountId,
                accountName = summary.accountName,
                balance = summary.balance,
                lastUpdated = summary.lastUpdated,
                eventVersion = summary.eventVersion
            )
        }
        return ResponseEntity.ok(accounts)
    }

    @GetMapping("/{accountId}")
    fun getAccount(@PathVariable accountId: UUID): ResponseEntity<AccountSummaryResponse> {
        val summary = repository.findById(accountId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            AccountSummaryResponse(
                accountId = summary.accountId,
                accountName = summary.accountName,
                balance = summary.balance,
                lastUpdated = summary.lastUpdated,
                eventVersion = summary.eventVersion
            )
        )
    }
}
