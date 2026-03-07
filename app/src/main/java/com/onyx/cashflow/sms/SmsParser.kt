package com.onyx.cashflow.sms

import com.onyx.cashflow.data.TransactionType

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val type: TransactionType
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

    // Bank of Baroda - UPI Debit
    // "Rs.1125.00 Dr. from A/C XXXXXX1558 and Cr. to wwwdhruvjaiswalcom0@oksbi. Ref:606678307558..."
    private val BOB_DEBIT_UPI = Regex(
        """Rs\.?([\d,]+\.?\d*)\s*Dr\.\s*from\s*A/C\s*\S+\s*and\s*Cr\.\s*to\s+(\S+?)\.?\s*Ref:""",
        RegexOption.IGNORE_CASE
    )

    private fun parseBobDebitUpi(body: String): ParsedSms? {
        val match = BOB_DEBIT_UPI.find(body) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val merchant = cleanMerchant(match.groupValues[2])
        return ParsedSms(amount, merchant, TransactionType.EXPENSE)
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
        return ParsedSms(amount, merchant, TransactionType.INCOME)
    }

    // Bank of Baroda - Clearing Credit
    // "Rs.100000 Credited to A/c ...1558 through Clearing. Total Bal:Rs.264914.55CR..."
    private val BOB_CREDIT_CLEARING = Regex(
        """Rs\.?([\d,]+\.?\d*)\s*Credited\s+to\s+A/c\s+\S+\s+through\s+(\S+)\.""",
        RegexOption.IGNORE_CASE
    )

    private fun parseBobCreditClearing(body: String): ParsedSms? {
        val match = BOB_CREDIT_CLEARING.find(body) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val method = match.groupValues[2]
        return ParsedSms(amount, "Transfer via $method", TransactionType.INCOME)
    }

    // Generic fallback - catches "Rs" or "INR" amounts with debit/credit context
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
        // Try debit patterns first
        GENERIC_DEBIT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown merchant", TransactionType.EXPENSE)
        }
        GENERIC_DEBIT_ALT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown merchant", TransactionType.EXPENSE)
        }
        // Then credit
        GENERIC_CREDIT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown source", TransactionType.INCOME)
        }
        GENERIC_CREDIT_ALT.find(body)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedSms(amount, "Unknown source", TransactionType.INCOME)
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
