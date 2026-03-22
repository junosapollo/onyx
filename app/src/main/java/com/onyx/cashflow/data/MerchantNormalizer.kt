package com.onyx.cashflow.data

/**
 * Normalizes merchant strings to produce stable keys for matching.
 * Handles UPI IDs, app suffixes, and formatting variations.
 */
object MerchantNormalizer {

    // UPI domain suffixes to strip
    private val UPI_DOMAIN = Regex("""@\w+$""")

    // App source suffixes like "(via Google Pay)", "(via PhonePe)"
    private val VIA_SUFFIX = Regex("""\s*\(via\s+.*\)$""", RegexOption.IGNORE_CASE)

    // Common prefixes in UPI IDs
    private val COMMON_PREFIXES = Regex("""^(www|http[s]?|merchant|pay|upi)""", RegexOption.IGNORE_CASE)

    /**
     * Produces a stable normalized key from a merchant string.
     * Examples:
     *   "wwwdhruvjaiswalcom0@oksbi" → "dhruvjaiswalcom0"
     *   "Dhruv Jaiswal (via Google Pay)" → "dhruvjaiswal"
     *   "merchant@ybl" → "merchant"
     *   "Unknown merchant" → "unknownmerchant"
     */
    fun normalizeKey(merchant: String): String {
        var normalized = merchant.trim()

        // Strip "(via ...)" suffix
        normalized = VIA_SUFFIX.replace(normalized, "")

        // Strip UPI domain (@oksbi, @ybl, etc.)
        normalized = UPI_DOMAIN.replace(normalized, "")

        // Lowercase
        normalized = normalized.lowercase()

        // Remove common prefixes
        normalized = COMMON_PREFIXES.replace(normalized, "")

        // Remove all non-alphanumeric characters (dots, hyphens, spaces, etc.)
        normalized = normalized.replace(Regex("[^a-z0-9]"), "")

        return normalized.ifEmpty { merchant.lowercase().replace(Regex("[^a-z0-9]"), "") }
    }

    /**
     * Extracts a human-readable display name from a merchant string.
     * Strips UPI domains and app suffixes but keeps spaces and capitalization.
     */
    fun displayName(merchant: String): String {
        var name = merchant.trim()
        name = VIA_SUFFIX.replace(name, "")
        name = UPI_DOMAIN.replace(name, "")
        return name.trim().ifEmpty { merchant.trim() }
    }
}
