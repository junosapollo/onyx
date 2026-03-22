package com.onyx.cashflow.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo

class PhonePeParser : AppParser {
    override fun parse(rootNode: AccessibilityNodeInfo): ParsedTransaction? {
        // PhonePe typically shows "Payment of ₹X to Y successful." in a single node,
        // or uses "₹X" and "Paid to Y" similar to GPay. We'll check for "successful" to verify screen.

        val successNodes = rootNode.findAccessibilityNodeInfosByText("successful")
        if (successNodes.isNullOrEmpty()) {
             // Maybe it's capitalized
             val successNodesCapital = rootNode.findAccessibilityNodeInfosByText("Successful")
             if (successNodesCapital.isNullOrEmpty()) return null
        }

        // Find amount
        val amountNodes = rootNode.findAccessibilityNodeInfosByText("₹")
        if (amountNodes.isNullOrEmpty()) return null

        var amount: Double? = null
        for (node in amountNodes) {
            val text = node.text?.toString() ?: continue
            val match = Regex("""₹\s*([0-9,.]+)""").find(text)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null) break
            }
        }
        if (amount == null) return null

        // Find Merchant
        var merchantName = "Unknown"
        val paidToNodes = rootNode.findAccessibilityNodeInfosByText("Paid to")
        if (!paidToNodes.isNullOrEmpty()) {
            val text = paidToNodes[0].text?.toString() ?: ""
            merchantName = text.replace("Paid to", "").trim()
        }

        return ParsedTransaction(amount, merchantName, "PhonePe")
    }
}
