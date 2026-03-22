package com.onyx.cashflow.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo

class GPayParser : AppParser {
    override fun parse(rootNode: AccessibilityNodeInfo): ParsedTransaction? {
        // Find text nodes containing "₹"
        val amountNodes = rootNode.findAccessibilityNodeInfosByText("₹")
        if (amountNodes.isNullOrEmpty()) return null

        var amount: Double? = null
        for (node in amountNodes) {
            val text = node.text?.toString() ?: continue
            // Extract numbers from "₹500.00"
            val match = Regex("""₹\s*([0-9,.]+)""").find(text)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null) break
            }
        }

        if (amount == null) return null

        // Try to find the recipient "Paid to ___"
        var merchantName = "Unknown"
        val paidToNodes = rootNode.findAccessibilityNodeInfosByText("Paid to")
        if (!paidToNodes.isNullOrEmpty()) {
            val text = paidToNodes[0].text?.toString() ?: ""
            merchantName = text.replace("Paid to", "").trim()
        } else {
            val allTexts = mutableListOf<String>()
            fun traverse(node: AccessibilityNodeInfo?) {
                if (node == null) return
                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()
                val viewId = node.viewIdResourceName ?: "no_id"

                if (!text.isNullOrEmpty()) {
                    android.util.Log.d("GPayParserTree", "Text Node: '$text', ViewID: $viewId")
                    allTexts.add(text)
                }
                if (!desc.isNullOrEmpty()) {
                    android.util.Log.d("GPayParserTree", "Desc Node: '$desc', ViewID: $viewId")
                    allTexts.add(desc)
                }
                
                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i))
                }
            }
            traverse(rootNode)
            
            val ignoreList = setOf("completed", "processing", "success", "successful", "view details", "done", "share", "checking balance", "rupees", "bank", "account", "secure connection", "powered by upi", "payment started", "payment processing", "got it", "logo", "profile", "avatar", "image", "close")
            val amountPattern = Regex("""^[0-9,.]+$""")
            val timePattern = Regex("""^\d{1,2}:\d{2}\s*(AM|PM|am|pm)?$""")
            val datePattern = Regex("""^\d{1,2}\s+[a-zA-Z]{3}.*""")

            for (text in allTexts) {
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

        // We only consider it a valid transaction if we found an amount and some confirmation of success.
        // GPay uses words like "Processing" or "Completed".
        // A simple check is just ensuring it's not a generic screen.
        // We'll trust the caller to verify the package name.

        return ParsedTransaction(amount, merchantName, "Google Pay")
    }
}
