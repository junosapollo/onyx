package com.onyx.cashflow.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo

class GPayParser : AppParser {

    override fun parse(rootNode: AccessibilityNodeInfo): ParsedTransaction? {
        // Legacy: extract texts from node tree, then delegate to text-based parser
        val texts = mutableListOf<String>()
        extractTexts(rootNode, texts)
        return parseFromTexts(texts)
    }

    override fun parseFromTexts(texts: List<String>): ParsedTransaction? {
        // Find amount from text containing "₹"
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

        // Try to find "Paid to ___"
        var merchantName = "Unknown"
        for (text in texts) {
            if (text.contains("Paid to", ignoreCase = true)) {
                merchantName = text.replace(Regex("(?i)paid to"), "").trim()
                if (merchantName.isNotEmpty()) break
            }
        }

        // Fallback: find first meaningful text
        if (merchantName == "Unknown") {
            val ignoreList = setOf(
                "completed", "processing", "success", "successful",
                "view details", "done", "share", "checking balance",
                "rupees", "bank", "account", "secure connection",
                "powered by upi", "payment started", "payment processing",
                "got it", "logo", "profile", "avatar", "image", "close",
                // Common UI buttons that might slip through
                "back", "report user", "report", "home", "more options",
                "more", "help", "settings", "cancel", "ok", "yes", "no",
                "retry", "new payment", "go to home", "send again",
                "check balance", "split", "request", "pay", "scan",
                "navigate up", "open navigation drawer"
            )
            val amountPattern = Regex("""^[0-9,.]+$""")
            val timePattern = Regex("""^\d{1,2}:\d{2}\s*(AM|PM|am|pm)?$""")
            val datePattern = Regex("""^\d{1,2}\s+[a-zA-Z]{3}.*""")

            for (text in texts) {
                val lower = text.lowercase()
                if (ignoreList.contains(lower)) continue
                if (lower.contains("₹")) continue
                if (amountPattern.matches(text)) continue
                if (timePattern.matches(text)) continue
                if (datePattern.matches(text)) continue
                if (lower.contains("upi id")) continue
                if (lower.contains("transaction id")) continue
                if (lower.startsWith("paid to")) continue
                if (lower.contains("bank")) continue
                if (lower.contains("account")) continue
                if (lower.contains("ending in")) continue
                if (lower.contains("card")) continue
                if (lower.length < 2) continue

                merchantName = text
                break
            }
        }

        return ParsedTransaction(amount, merchantName, "Google Pay")
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
