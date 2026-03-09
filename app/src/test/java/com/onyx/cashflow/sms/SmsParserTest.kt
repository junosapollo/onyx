package com.onyx.cashflow.sms

import com.onyx.cashflow.data.TransactionType
import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    // --- BOB Debit UPI ---

    @Test
    fun `parse BOB debit UPI with balance`() {
        val sms = "Rs.1125.00 Dr. from A/C XXXXXX1558 and Cr. to merchant@oksbi. Ref:606678307558. Bal:Rs.10000.55CR"
        val result = SmsParser.parse(sms)
        assertNotNull(result)
        assertEquals(1125.0, result!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("merchant@oksbi", result.merchant)
        assertEquals(10000.55, result.balance!!, 0.01)
        assertEquals("1558", result.accountNumber)
    }

    @Test
    fun `parse BOB debit UPI without balance`() {
        val sms = "Rs.500.00 Dr. from A/C XXXXXX1558 and Cr. to shop@upi. Ref:123456789012."
        val result = SmsParser.parse(sms)
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertNull(result.balance)
        assertEquals("1558", result.accountNumber)
    }

    // --- BOB Credit Clearing ---

    @Test
    fun `parse BOB clearing credit with balance`() {
        val sms = "Rs.100000 Credited to A/c ...1558 through Clearing. Total Bal:Rs.264914.55CR"
        val result = SmsParser.parse(sms)
        assertNotNull(result)
        assertEquals(100000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("Transfer via Clearing", result.merchant)
        assertEquals(264914.55, result.balance!!, 0.01)
        assertEquals("1558", result.accountNumber)
    }

    // --- Generic with balance ---

    @Test
    fun `parse generic debit with Avl Bal`() {
        val sms = "Rs.2500.00 debited from A/C XX4321 on 09-03-2026. Avl Bal: Rs.15000.75"
        val result = SmsParser.parse(sms)
        assertNotNull(result)
        assertEquals(2500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(15000.75, result.balance!!, 0.01)
        assertEquals("4321", result.accountNumber)
    }

    @Test
    fun `parse generic credit with INR balance`() {
        val sms = "INR 5000.00 credited to A/C 9876543210 on 09-03-2026. Avl Bal INR 25000.00"
        val result = SmsParser.parse(sms)
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(25000.0, result.balance!!, 0.01)
    }

    // --- Balance and account extraction ---

    @Test
    fun `extractBalance returns null when no balance present`() {
        val sms = "Your OTP for login is 123456"
        assertNull(SmsParser.extractBalance(sms))
    }

    @Test
    fun `extractBalance handles various formats`() {
        assertEquals(10000.55, SmsParser.extractBalance("Bal:Rs.10000.55CR")!!, 0.01)
        assertEquals(25000.0, SmsParser.extractBalance("Avl Bal: Rs.25000.00")!!, 0.01)
        assertEquals(15000.75, SmsParser.extractBalance("Total Bal:Rs.15,000.75")!!, 0.01)
        assertEquals(5000.0, SmsParser.extractBalance("Avl Bal INR 5000.00")!!, 0.01)
    }

    @Test
    fun `extractAccountNumber returns last 4 digits`() {
        assertEquals("1558", SmsParser.extractAccountNumber("A/C XXXXXX1558"))
        assertEquals("1558", SmsParser.extractAccountNumber("A/c ...1558"))
        assertEquals("4321", SmsParser.extractAccountNumber("A/C XX4321"))
    }

    @Test
    fun `extractAccountNumber returns null for no account`() {
        assertNull(SmsParser.extractAccountNumber("Your OTP is 123456"))
    }

    // --- Non-transaction SMS ---

    @Test
    fun `parse returns null for non-transaction SMS`() {
        assertNull(SmsParser.parse("Your OTP for login is 123456"))
        assertNull(SmsParser.parse("Welcome to our bank. Call us at 1800-XXX-XXXX"))
    }
}
