package com.onyx.cashflow.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo

// Represents the data extracted from a payment success screen
data class ParsedTransaction(
    val amount: Double,
    val merchantName: String,
    val appName: String
)

interface AppParser {
    // Returns a ParsedTransaction if the current screen is a valid "Success" screen and data is found
    fun parse(rootNode: AccessibilityNodeInfo): ParsedTransaction?
}
