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
                val isTrusted = db.trustedSenderDao().isTrusted(sender)

                if (isTrusted) {
                    // Auto-log: find "Other" category by name
                    val cats = db.categoryDao().getAll().first()
                    val otherCategoryId = cats.find {
                        it.name.equals("Other", ignoreCase = true)
                    }?.id

                    db.transactionDao().insert(
                        Transaction(
                            amount = parsed.amount,
                            categoryId = otherCategoryId,
                            note = parsed.merchant,
                            date = System.currentTimeMillis(),
                            type = parsed.type
                        )
                    )
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
