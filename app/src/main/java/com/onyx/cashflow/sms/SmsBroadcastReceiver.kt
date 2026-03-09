package com.onyx.cashflow.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.PendingTransaction
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        // Regular phone numbers: 10+ digits, optionally prefixed with + or country code
        // Examples to REJECT: +919876543210, 09876543210, 9876543210
        // Examples to ALLOW: BOB, HDFCBK, AD-BOBSMS, VM-BOBANK, JD-SBIINB
        private val PHONE_NUMBER_PATTERN = Regex("""^\+?\d{7,15}$""")

        /**
         * Verified core sender IDs for major Indian banks.
         * The core ID is stable but the prefix/suffix change based on network
         * routing (e.g., VM-BOBSMS-S, JK-BOBSMS-S, JX-BOBSMS-S).
         * We match these as substrings (case-insensitive) to handle all variants.
         */
        private val VERIFIED_BANK_CORE_IDS = setOf(
            // HDFC Bank
            "HDFCBK", "HDFCBN",
            // ICICI Bank
            "ICICIB",
            // Axis Bank
            "AXISBK",
            // Kotak Mahindra Bank
            "KOTAKB",
            // IDFC First Bank
            "IDFCFB",
            // IndusInd Bank
            "INDUSI",
            // Yes Bank
            "YESBNK",
            // RBL Bank
            "RBLBNK",
            // South Indian Bank
            "SIBBNK",
            // State Bank of India (SBI)
            "SBIINB", "SBIPSG", "SBICRD", "SBIBNK", "SBYONO", "ATMSBI",
            // Bank of Baroda (BoB)
            "BOBTXN", "BOBMSG", "BOBSMS",
            // Punjab National Bank (PNB)
            "PNBMSG", "PNBSMS",
            // Canara Bank
            "CANBNK",
            // Union Bank of India
            "UBININ",
            // Bank of India (BoI)
            "BOIIND", "BOISMS",
            // Central Bank of India
            "CBISMS", "CBIBNK",
            // Indian Bank
            "INDBNK",
            // Standard Chartered
            "SCBLIN",
            // Citi Bank
            "CITIBK",
            // HSBC India
            "HSBCIN",
            // American Express
            "AMEXIN"
        )

        /**
         * Returns true if the sender looks like a service/bank shortcode (alphanumeric).
         * Returns false for regular phone numbers (all digits).
         */
        fun isServiceSender(address: String): Boolean {
            val cleaned = address.trim()
            if (cleaned.isEmpty()) return false
            // If it matches a pure phone number pattern, it's NOT a service sender
            if (PHONE_NUMBER_PATTERN.matches(cleaned)) return false
            // Must contain at least one letter to be an alphanumeric service ID
            return cleaned.any { it.isLetter() }
        }

        /**
         * Returns true if the sender address contains any verified bank core ID.
         * Handles prefix/suffix variations like VM-BOBSMS-S, JK-HDFCBK, etc.
         */
        fun containsVerifiedBankId(address: String): Boolean {
            val upper = address.uppercase()
            return VERIFIED_BANK_CORE_IDS.any { coreId -> upper.contains(coreId) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group message parts by sender (multi-part SMS)
        val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }

        grouped.forEach { (sender, parts) ->
            // Skip regular phone numbers — only process service/bank senders
            if (!isServiceSender(sender)) return@forEach

            val fullBody = parts.joinToString("") { it.messageBody ?: "" }
            if (fullBody.isBlank()) return@forEach

            val parsed = SmsParser.parse(fullBody) ?: return@forEach

            val db = AppDatabase.getInstance(context)

            CoroutineScope(Dispatchers.IO).launch {
                // Check verified bank ID list first, then fall back to user-approved senders
                val isTrusted = containsVerifiedBankId(sender)
                        || db.trustedSenderDao().isTrusted(sender)

                if (isTrusted) {
                    // Check for existing merchant category rule
                    val rule = db.merchantCategoryRuleDao().getRuleForMerchant(parsed.merchant.trim())
                    
                    val categoryIdToAssign = if (rule != null) {
                        rule.categoryId
                    } else {
                        // Fallback to "Other" category
                        val cats = db.categoryDao().getAll().first()
                        cats.find { it.name.equals("Other", ignoreCase = true) }?.id
                    }

                    db.transactionDao().insert(
                        Transaction(
                            amount = parsed.amount,
                            categoryId = categoryIdToAssign,
                            note = parsed.merchant,
                            date = System.currentTimeMillis(),
                            type = parsed.type
                        )
                    )

                    // Check for balance gaps (missed transactions)
                    BalanceGapDetector.checkForGap(parsed, sender, db)
                } else {
                    // Unknown sender → pending queue
                    db.pendingTransactionDao().insert(
                        PendingTransaction(
                            amount = parsed.amount,
                            merchant = parsed.merchant,
                            senderAddress = sender,
                            rawSms = fullBody,
                            date = System.currentTimeMillis(),
                            type = parsed.type
                        )
                    )
                }
            }
        }
    }
}
