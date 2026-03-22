package com.onyx.cashflow.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.onyx.cashflow.accessibility.parsers.AppParser
import com.onyx.cashflow.accessibility.parsers.GPayParser
import com.onyx.cashflow.accessibility.parsers.ParsedTransaction
import com.onyx.cashflow.accessibility.parsers.PhonePeParser
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.MerchantNormalizer
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PaymentAccessibilityService : AccessibilityService() {

    private val parsers = mutableMapOf<String, AppParser>()
    private var lastSavedSignature = ""
    private var lastProcessTime = 0L

    // Dedicated background thread for all heavy work
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("PaymentAccessibility", "Service Connected")

        parsers["com.google.android.apps.nbu.paisa.user"] = GPayParser()
        parsers["com.phonepe.app"] = PhonePeParser()

        // Start dedicated background thread
        workerThread = HandlerThread("PaymentParserThread").apply { start() }
        workerHandler = Handler(workerThread.looper)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Filter by package name first — fast, no allocations
        val packageName = event.packageName?.toString() ?: return
        val parser = parsers[packageName] ?: return

        // Code-level debounce: 2 seconds
        val now = System.currentTimeMillis()
        if (now - lastProcessTime < 2000L) return
        lastProcessTime = now

        // ── MAIN THREAD: fast text extraction ──
        // Extract all text strings from the node tree into a plain list.
        // This is just string copies — microseconds, no regex or DB.
        val rootNode = rootInActiveWindow ?: return
        val textSnapshot = mutableListOf<String>()
        extractTexts(rootNode, textSnapshot)

        if (textSnapshot.isEmpty()) return

        // ── BACKGROUND THREAD: all heavy work ──
        workerHandler.post {
            try {
                val parsedTx = parser.parseFromTexts(textSnapshot)
                if (parsedTx != null) {
                    saveTransaction(parsedTx)
                }
            } catch (e: Exception) {
                Log.e("PaymentAccessibility", "Error parsing", e)
            }
        }
    }

    /**
     * Recursively extract text from static (non-clickable) nodes only.
     * Buttons, image views, and clickable elements are skipped.
     * Must be called on the main thread while nodes are still valid.
     */
    private fun extractTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        try {
            // Skip clickable nodes (buttons like "Back", "Report user", "Share")
            if (node.isClickable) return

            // Skip image views and button-class nodes
            val className = node.className?.toString() ?: ""
            if (className.contains("ImageView") ||
                className.contains("ImageButton") ||
                className.contains("Button") && !className.contains("RadioButton")) {
                return
            }

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrEmpty()) out.add(text)
            if (!desc.isNullOrEmpty()) out.add(desc)
            for (i in 0 until node.childCount) {
                extractTexts(node.getChild(i), out)
            }
        } catch (e: Exception) {
            // Node may have been recycled mid-traversal — safe to ignore
        }
    }

    /**
     * Saves a parsed transaction to the database.
     * Called on the worker thread — safe to do DB I/O here.
     */
    private fun saveTransaction(parsedTx: ParsedTransaction) {
        // Dedup by amount + minute only — no merchant name, since the same screen
        // should never produce two legitimate transactions in the same minute
        val sig = "${parsedTx.amount}-${System.currentTimeMillis() / 60000}"

        if (sig == lastSavedSignature) return
        lastSavedSignature = sig

        Log.d("PaymentAccessibility", "Parsed Transaction: $parsedTx")

        runBlocking {
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
                        Log.d("PaymentAccessibility", "Updated duplicate with better merchant name")
                    } else {
                        Log.d("PaymentAccessibility", "Skipped duplicate transaction")
                    }
                    break
                }
            }

            if (isTrueDuplicate) return@runBlocking

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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }
    }

    override fun onInterrupt() {
        Log.e("PaymentAccessibility", "Service Interrupted")
    }
}
