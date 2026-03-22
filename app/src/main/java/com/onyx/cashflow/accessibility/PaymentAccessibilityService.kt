package com.onyx.cashflow.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.onyx.cashflow.accessibility.parsers.AppParser
import com.onyx.cashflow.accessibility.parsers.GPayParser
import com.onyx.cashflow.accessibility.parsers.PhonePeParser
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.MerchantNormalizer
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val parsers = mutableMapOf<String, AppParser>()
    private var lastSavedSignature = "" // Prevent duplicate saves
    private var lastProcessTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("PaymentAccessibility", "Service Connected")
        
        parsers["com.google.android.apps.nbu.paisa.user"] = GPayParser()
        parsers["com.phonepe.app"] = PhonePeParser()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only process window state changes (screen transitions)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // Filter by package name first
        val packageName = event.packageName?.toString() ?: return
        val parser = parsers[packageName] ?: return

        // Debounce: 2 seconds — payment success screens stay visible for several seconds
        val now = System.currentTimeMillis()
        if (now - lastProcessTime < 2000L) return
        lastProcessTime = now

        // Capture root node reference and parse on background thread
        val rootNode = rootInActiveWindow ?: return

        scope.launch {
            try {
                val parsedTx = parser.parse(rootNode)
                if (parsedTx != null) {
                    saveTransaction(parsedTx)
                }
            } catch (e: Exception) {
                Log.e("PaymentAccessibility", "Error parsing", e)
            }
        }
    }

    private suspend fun saveTransaction(parsedTx: com.onyx.cashflow.accessibility.parsers.ParsedTransaction) {
        val sig = "${parsedTx.amount}-${parsedTx.merchantName}-${System.currentTimeMillis() / 60000}" // minute-level dedupe
        
        if (sig == lastSavedSignature) return
        lastSavedSignature = sig

        Log.d("PaymentAccessibility", "Parsed Transaction: $parsedTx")

        val dao = AppDatabase.getInstance(applicationContext).transactionDao()
        
        val twoMinsAgo = System.currentTimeMillis() - (2 * 60 * 1000)
        val duplicates = dao.findRecentDuplicates(parsedTx.amount, TransactionType.EXPENSE, twoMinsAgo)
        val newNote = "${parsedTx.merchantName} (via ${parsedTx.appName})"

        var isTrueDuplicate = false
        for (duplicate in duplicates) {
            val existingMerchant = duplicate.note.replace(Regex("""\s*\(via .*\)$"""), "").trim()
            val incomingMerchant = parsedTx.merchantName.trim()

            if (existingMerchant.equals(incomingMerchant, ignoreCase = true) || 
                existingMerchant.contains("Unknown", ignoreCase = true) || 
                incomingMerchant.contains("Unknown", ignoreCase = true)) {
                
                isTrueDuplicate = true

                if (duplicate.note.contains("Unknown", ignoreCase = true) && !parsedTx.merchantName.contains("Unknown", ignoreCase = true)) {
                    dao.update(duplicate.copy(note = newNote))
                    Log.d("PaymentAccessibility", "Updated existing duplicate transaction with better merchant name")
                } else {
                    Log.d("PaymentAccessibility", "Skipped duplicate transaction")
                }
                break
            }
        }

        if (isTrueDuplicate) return

        // Look up category rule using normalized key
        val db = AppDatabase.getInstance(applicationContext)
        val normalizedKey = MerchantNormalizer.normalizeKey(parsedTx.merchantName.trim())
        val rule = db.merchantCategoryRuleDao().getRuleForNormalizedKey(normalizedKey)
        val categoryId = if (rule != null) {
            rule.categoryId
        } else {
            val cats = db.categoryDao().getAll().first()
            cats.find { it.name.equals("Other", ignoreCase = true) }?.id
        }

        val transaction = Transaction(
            amount = parsedTx.amount,
            categoryId = categoryId,
            note = newNote,
            date = System.currentTimeMillis(),
            type = TransactionType.EXPENSE
        )
        dao.insert(transaction)
        Log.d("PaymentAccessibility", "Saved transaction to DB")
    }

    override fun onInterrupt() {
        Log.e("PaymentAccessibility", "Service Interrupted")
    }
}
