package com.onyx.cashflow.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.HandlerThread
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
import com.onyx.cashflow.utils.OnyxLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val TAG = "AccessibilitySvc"

class PaymentAccessibilityService : AccessibilityService() {

    private val parsers = mutableMapOf<String, AppParser>()
    private var lastSavedSignature = ""
    private var lastProcessTime = 0L

    // Dedicated background thread for all heavy work
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler

    override fun onServiceConnected() {
        super.onServiceConnected()
        OnyxLogger.i(TAG, "Service connected — registering parsers")

        parsers["com.google.android.apps.nbu.paisa.user"] = GPayParser()
        parsers["com.phonepe.app"] = PhonePeParser()

        OnyxLogger.i(TAG, "Listening for packages: ${parsers.keys.joinToString()}")

        // Start dedicated background thread
        workerThread = HandlerThread("PaymentParserThread").apply { start() }
        workerHandler = Handler(workerThread.looper)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Filter by package name first — fast, no allocations
        val packageName = event.packageName?.toString() ?: return
        val parser = parsers[packageName]
        if (parser == null) {
            // Not a package we care about — silent drop (very frequent, don't log to file)
            return
        }

        OnyxLogger.d(TAG, "Event received from $packageName — type=${event.eventType}")

        // Code-level debounce: 2 seconds
        val now = System.currentTimeMillis()
        val elapsed = now - lastProcessTime
        if (elapsed < 2000L) {
            OnyxLogger.d(TAG, "Debounce DROP: only ${elapsed}ms since last event (threshold=2000ms)")
            return
        }
        lastProcessTime = now

        // ── MAIN THREAD: fast text extraction ──
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            OnyxLogger.e(TAG, "rootInActiveWindow is null — window may have closed before extraction")
            return
        }

        val textSnapshot = mutableListOf<String>()
        extractTexts(rootNode, textSnapshot)

        if (textSnapshot.isEmpty()) {
            OnyxLogger.d(TAG, "Text snapshot is empty — no parseable content found in window")
            return
        }

        OnyxLogger.d(TAG, "Text snapshot (${textSnapshot.size} strings) captured. Dispatching to worker thread.")

        // ── BACKGROUND THREAD: all heavy work ──
        workerHandler.post {
            try {
                val parsedTx = parser.parseFromTexts(textSnapshot)
                if (parsedTx != null) {
                    OnyxLogger.i(TAG, "Parser MATCH: amount=${parsedTx.amount}, merchant='${parsedTx.merchantName}', app=${parsedTx.appName}")
                    saveTransaction(parsedTx, textSnapshot)
                } else {
                    OnyxLogger.d(TAG, "Parser MISS — no transaction found in snapshot. Snapshot: $textSnapshot")
                }
            } catch (e: Exception) {
                OnyxLogger.e(TAG, "Exception during parsing", e)
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
            // Node may have been recycled mid-traversal — common at screen transitions
            OnyxLogger.d(TAG, "Node recycled during traversal (depth=${out.size}): ${e.message}")
        }
    }

    /**
     * Saves a parsed transaction to the database.
     * Called on the worker thread — safe to do DB I/O here.
     *
     * [rawSnapshot] is logged for debugging parse discrepancies.
     * Only amounts are logged at INFO level; full snapshot is DEBUG only.
     */
    private fun saveTransaction(parsedTx: ParsedTransaction, rawSnapshot: List<String>) {
        // In-memory dedup by amount + minute
        val sig = "${parsedTx.amount}-${System.currentTimeMillis() / 60000}"
        if (sig == lastSavedSignature) {
            OnyxLogger.d(TAG, "In-memory dedup HIT for sig=$sig — skipping DB query")
            return
        }
        lastSavedSignature = sig

        OnyxLogger.i(TAG, "Processing transaction: amount=${parsedTx.amount}, app=${parsedTx.appName}")
        OnyxLogger.d(TAG, "Raw snapshot used for parse: $rawSnapshot")

        runBlocking {
            val dao = AppDatabase.getInstance(applicationContext).transactionDao()

            val twoMinsAgo = System.currentTimeMillis() - (2 * 60 * 1000)
            val duplicates = dao.findRecentDuplicates(parsedTx.amount, TransactionType.EXPENSE, twoMinsAgo)
            val newNote = "${parsedTx.merchantName} (via ${parsedTx.appName})"

            OnyxLogger.d(TAG, "DB dedup check: ${duplicates.size} candidate(s) in last 2 mins")

            var isTrueDuplicate = false
            for (duplicate in duplicates) {
                val existingMerchant = duplicate.note.replace(Regex("""\s*\(via .*\)$"""), "").trim()
                val incomingMerchant = parsedTx.merchantName.trim()

                if (existingMerchant.equals(incomingMerchant, ignoreCase = true) ||
                    existingMerchant.contains("Unknown", ignoreCase = true) ||
                    incomingMerchant.contains("Unknown", ignoreCase = true)) {

                    isTrueDuplicate = true

                    if (duplicate.note.contains("Unknown", ignoreCase = true) &&
                        !parsedTx.merchantName.contains("Unknown", ignoreCase = true)) {
                        dao.update(duplicate.copy(note = newNote))
                        OnyxLogger.i(TAG, "Duplicate UPDATED with better merchant: '$existingMerchant' → '${parsedTx.merchantName}'")
                    } else {
                        OnyxLogger.i(TAG, "True duplicate SKIPPED: existing='$existingMerchant', incoming='$incomingMerchant'")
                    }
                    break
                }
            }

            if (isTrueDuplicate) return@runBlocking

            // Category lookup
            val db = AppDatabase.getInstance(applicationContext)
            val normalizedKey = MerchantNormalizer.normalizeKey(parsedTx.merchantName.trim())
            val rule = db.merchantCategoryRuleDao().getRuleForNormalizedKey(normalizedKey)
            val categoryId = if (rule != null) {
                OnyxLogger.d(TAG, "Category rule HIT: key='$normalizedKey' → categoryId=${rule.categoryId}")
                rule.categoryId
            } else {
                OnyxLogger.d(TAG, "Category rule MISS for key='$normalizedKey' — falling back to 'Other'")
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
            OnyxLogger.i(TAG, "Transaction SAVED: amount=${parsedTx.amount}, note='$newNote', categoryId=$categoryId")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        OnyxLogger.i(TAG, "Service destroyed — shutting down worker thread")
        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }
    }

    override fun onInterrupt() {
        OnyxLogger.e(TAG, "Service INTERRUPTED by system")
    }
}
