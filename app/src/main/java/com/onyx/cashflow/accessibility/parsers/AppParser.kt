package com.onyx.cashflow.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo

// Represents the data extracted from a payment success screen
data class ParsedTransaction(
    val amount: Double,
    val merchantName: String,
    val appName: String
)

interface AppParser {
    // Legacy: parse directly from node tree (kept for compatibility)
    fun parse(rootNode: AccessibilityNodeInfo): ParsedTransaction?

    // Parse from pre-extracted text snapshot (no node tree access needed)
    fun parseFromTexts(texts: List<String>): ParsedTransaction?
}
