package com.onyx.cashflow.sms

import com.onyx.cashflow.data.TransactionType

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val type: TransactionType,
    val balance: Double? = null,
    val accountNumber: String? = null
)

object SmsParser {

    /**
     * Attempts to parse a bank transaction SMS.
     * Returns ParsedSms if successful, null if the SMS is not a recognized transaction.
     */
    fun parse(body: String): ParsedSms? {
        return parseBobDebitUpi(body)
            ?: parseBobCreditUpi(body)
            ?: parseBobCreditClearing(body)
            ?: parseGeneric(body)
    }

    // --- Balance extraction (shared) ---

    // Matches patterns like: "Bal:Rs.10000.55CR", "Avl Bal:Rs.10000.55", "Total Bal:Rs.264914.55CR"
    // Also: "Avl Bal INR 1234.56", "Balance: Rs 1234.56", "Avl Bal is Rs.1234"
    private val BALANCE_PATTERN = Regex(
        """(?:Avl\.?\s*Bal(?:ance)?|Total\s*Bal|Bal)\s*(?:is\s*)?[:\s]*(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    // Matches masked account numbers like: "A/C XXXXXX1558", "A/c XX1234", "A/C ...1558", "a/c *1234"
    private val ACCOUNT_PATTERN = Regex(
        """A/[Cc]\s*[.:]*\s*[Xx*.\s]*(\d{4,})""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extracts the available balance from an SMS body.
     */
    fun extractBalance(body: String): Double? {
        val match = BALANCE_PATTERN.find(body) ?: return null
        return parseAmount(match.groupValues[1])
    }

    /**
     * Extracts the last 4 digits of the account number from an SMS body.
     */
    fun extractAccountNumber(body: String): String? {
        val match = ACCOUNT_PATTERN.find(body) ?: return null
        val digits = match.groupValues[1]
        // Return last 4 digits as the account identifier
        return if (digits.length >= 4) digits.takeLast(4) else digits
    }

    // --- Bank of Baroda parsers ---

    // Bank of Baroda - UPI Debit
    // "Rs.1125.00 Dr. from A/C XXXXXX1558 and Cr. to wwwdhruvjaiswalcom0@oksbi. Ref:606678307558..."
    private val BOB_DEBIT_UPI = Regex(
        """Rs\.?([\d,]+\.?\d*)\s*Dr\.\s*from\s*A/C\s*(\S+)\s*and\s*Cr\.\s*to\s+(\S+?)\.\s*Ref:""",
        RegexOption.IGNORE_CASE
    )

    private fun parseBobDebitUpi(body: String): ParsedSms? {
        val match = BOB_DEBIT_UPI.find(body) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val accountRaw = match.groupValues[2]
        val merchant = cleanMerchant(match.groupValues[3])
        val accountNumber = accountRaw.filter { it.isDigit() }.takeLast(4).ifEmpty { null }
        val balance = extractBalance(body)
        return ParsedSms(amount, merchant, TransactionType.EXPENSE, balance, accountNumber)
    }

    // Bank of Baroda - UPI Credit
    // "Dear BOB UPI User: Your account is credited with INR 100.00 on 2026-02-23 11:23:53 PM by UPI Ref No..."
    private val BOB_CREDIT_UPI = Regex(
        """credited\s+with\s+INR\s+([\d,]+\.?\d*)\s+on\s+(.+?)\s+by\s+UPI\s+Ref\s+No\s+(\d+)""",
        RegexOption.IGNORE_CASE
    )

    private fun parseBobCreditUpi(body: String): ParsedSms? {
        val match = BOB_CREDIT_UPI.find(body) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val merchant = "UPI Transfer (Ref: ${match.groupValues[3]})"
        val balance = extractBalance(body)
        val accountNumber = extractAccountNumber(body)
        return ParsedSms(amount, merchant, TransactionType.INCOME, balance, accountNumber)
    }

    // Bank of Baroda - Clearing Credit
    // "Rs.100000 Credited to A/c ...1558 through Clearing. Total Bal:Rs.264914.55CR..."
    private val BOB_CREDIT_CLEARING = Regex(
        """Rs\.?([\d,]+\.?\d*)\s*Credited\s+to\s+A/c\s+(\S+)\s+through\s+(\S+)\.""",
        RegexOption.IGNORE_CASE
    )

    private fun parseBobCreditClearing(body: String): ParsedSms? {
        val match = BOB_CREDIT_CLEARING.find(body) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val accountRaw = match.groupValues[2]
        val method = match.groupValues[3]
        val accountNumber = accountRaw.filter { it.isDigit() }.takeLast(4).ifEmpty { null }
        val balance = extractBalance(body)
        return ParsedSms(amount, "Transfer via $method", TransactionType.INCOME, balance, accountNumber)
    }

    // --- Generic fallback parsers ---

    private val GENERIC_DEBIT = Regex(
        """(?:Rs\.?|INR)\s*([\d,]+\.?\d*).*?(?:debited|debit|dr\.|withdrawn|paid|spent)""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_CREDIT = Regex(
        """(?:Rs\.?|INR)\s*([\d,]+\.?\d*).*?(?:credited|credit|cr\.|received|deposited)""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_DEBIT_ALT = Regex(
        """(?:debited|debit|dr\.|withdrawn|paid|spent).*?(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_CREDIT_ALT = Regex(
        """(?:credited|credit|cr\.|received|deposited).*?(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    private fun parseGeneric(body: String): ParsedSms? {
        val balance = extractBalance(body)
        val accountNumber = extractAccountNumber(body)

        // Try debit patterns first
        GENERIC_DEBIT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown merchant", TransactionType.EXPENSE, balance, accountNumber)
        }
        GENERIC_DEBIT_ALT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown merchant", TransactionType.EXPENSE, balance, accountNumber)
        }
        // Then credit
        GENERIC_CREDIT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown source", TransactionType.INCOME, balance, accountNumber)
        }
        GENERIC_CREDIT_ALT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown source", TransactionType.INCOME, balance, accountNumber)
        }
        return null
    }

    private fun parseAmount(raw: String): Double? {
        return raw.replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }
    }

    private fun cleanMerchant(raw: String): String {
        // Remove trailing dots and clean up UPI IDs to be more readable
        return raw.trimEnd('.', ' ')
    }
}
