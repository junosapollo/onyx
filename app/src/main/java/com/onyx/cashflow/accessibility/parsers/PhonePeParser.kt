package com.onyx.cashflow.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo

class PhonePeParser : AppParser {

    override fun parse(rootNode: AccessibilityNodeInfo): ParsedTransaction? {
        // Legacy: extract texts from node tree, then delegate to text-based parser
        val texts = mutableListOf<String>()
        extractTexts(rootNode, texts)
        return parseFromTexts(texts)
    }

    override fun parseFromTexts(texts: List<String>): ParsedTransaction? {
        // Verify this is a success screen
        val hasSuccess = texts.any { it.contains("successful", ignoreCase = true) }
        if (!hasSuccess) return null

        // Find amount
        var amount: Double? = null
        for (text in texts) {
            if (!text.contains("₹")) continue
            val match = Regex("""₹\s*([0-9,.]+)""").find(text)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null) break
            }
        }
        if (amount == null) return null

        // Find Merchant
        var merchantName = "Unknown"
        for (text in texts) {
            if (text.contains("Paid to", ignoreCase = true)) {
                merchantName = text.replace(Regex("(?i)paid to"), "").trim()
                if (merchantName.isNotEmpty()) break
            }
        }

        return ParsedTransaction(amount, merchantName, "PhonePe")
    }

    private fun extractTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrEmpty()) out.add(text)
        if (!desc.isNullOrEmpty()) out.add(desc)
        for (i in 0 until node.childCount) {
            extractTexts(node.getChild(i), out)
        }
    }
}
