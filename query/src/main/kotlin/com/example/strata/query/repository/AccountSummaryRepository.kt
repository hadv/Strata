package com.example.strata.query.repository

import com.example.strata.query.model.AccountSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountSummaryRepository : JpaRepository<AccountSummary, UUID>
