package com.onyx.cashflow.sms

import com.onyx.cashflow.data.AccountBalance
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.BalanceGap
import com.onyx.cashflow.data.TransactionType
import kotlin.math.abs

/**
 * Detects missed transactions by comparing the balance reported in an SMS
 * against the expected balance calculated from the previous known balance
 * and the current transaction.
 *
 * Logic:
 *   1. If no balance or account number in the parsed SMS → skip (can't track)
 *   2. Look up the last known balance for this account
 *   3. If no previous record → store current balance (first time setup)
 *   4. Calculate expected balance:
 *        - EXPENSE: previousBalance - transactionAmount
 *        - INCOME:  previousBalance + transactionAmount
 *   5. Compare expected vs actual (reported in SMS)
 *   6. If |difference| > tolerance → create a BalanceGap record
 *   7. Always update stored balance to the new reported value
 */
object BalanceGapDetector {

    /**
     * Tolerance in rupees. Small differences (≤ ₹1) are ignored to
     * account for rounding in bank systems.
     */
    private const val TOLERANCE = 1.0

    /**
     * Checks for a balance gap and records it if found.
     *
     * @param parsed   The parsed SMS data including amount, type, balance, accountNumber
     * @param sender   The SMS sender address (for context)
     * @param db       The database instance
     */
    suspend fun checkForGap(
        parsed: ParsedSms,
        sender: String,
        db: AppDatabase
    ) {
        val reportedBalance = parsed.balance ?: return
        val accountId = parsed.accountNumber ?: return

        val balanceDao = db.accountBalanceDao()
        val gapDao = db.balanceGapDao()

        val previous = balanceDao.getByAccountId(accountId)

        if (previous != null) {
            // Calculate what the balance should be after this transaction
            val expectedBalance = when (parsed.type) {
                TransactionType.EXPENSE -> previous.lastBalance - parsed.amount
                TransactionType.INCOME -> previous.lastBalance + parsed.amount
            }

            val difference = reportedBalance - expectedBalance

            if (abs(difference) > TOLERANCE) {
                // Gap detected — there's an unaccounted transaction
                val gapType = if (difference < 0) {
                    // Actual balance is LOWER than expected → money went out (missed expense)
                    TransactionType.EXPENSE
                } else {
                    // Actual balance is HIGHER than expected → money came in (missed income)
                    TransactionType.INCOME
                }

                gapDao.insert(
                    BalanceGap(
                        accountId = accountId,
                        expectedBalance = expectedBalance,
                        actualBalance = reportedBalance,
                        gapAmount = abs(difference),
                        gapType = gapType
                    )
                )
            }
        }

        // Always update the stored balance to the latest reported value
        balanceDao.upsert(
            AccountBalance(
                accountId = accountId,
                lastBalance = reportedBalance,
                lastTransactionDate = System.currentTimeMillis(),
                bankSender = sender
            )
        )
    }
}
