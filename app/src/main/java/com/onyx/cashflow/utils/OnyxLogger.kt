package com.onyx.cashflow.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device file logger for remote debugging.
 *
 * - Thread-safe: all writes are synchronized on the file lock.
 * - Log rotation: trims the file to the last [MAX_LINES] lines when it exceeds [MAX_SIZE_BYTES].
 * - Crash capture: installs an UncaughtExceptionHandler that writes the stack trace before
 *   re-throwing to the default handler (so Android crash dialogs / Play Vitals still fire).
 * - Session marker: call [init] once in Application.onCreate to stamp a separator line.
 */
object OnyxLogger {

    private const val TAG = "OnyxLogger"
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "onyx_debug.txt"
    private const val MAX_SIZE_BYTES = 5 * 1024 * 1024L  // 5 MB
    private const val MAX_LINES = 10_000
    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /** File-level lock — shared by every write including the crash handler. */
    private val lock = Any()

    /** Cached reference set once in [init]. */
    @Volatile private var logFile: File? = null

    // ──────────────────────────────────────────────────────────────────────────
    // Initialisation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Call once from [android.app.Application.onCreate].
     * Sets up the log file, installs the crash handler, and writes a session header.
     */
    fun init(context: Context) {
        val dir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        val file = File(dir, LOG_FILE)
        logFile = file

        installCrashHandler()

        writeLine("INFO", "════════════════════════════════════════")
        writeLine("INFO", "SESSION START — ${DATE_FMT.format(Date())}")
        writeLine("INFO", "════════════════════════════════════════")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public logging API
    // ──────────────────────────────────────────────────────────────────────────

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLine("INFO ", "[$tag] $message")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLine("DEBUG", "[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val extra = throwable?.let { "\n  ${it.javaClass.simpleName}: ${it.message}" } ?: ""
        writeLine("ERROR", "[$tag] $message$extra")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Export / Clear helpers (called from SettingsScreen)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds an ACTION_SEND intent that opens the native Share Sheet with the log file attached.
     * Returns null if the log file does not exist yet.
     */
    fun buildExportIntent(context: Context): Intent? {
        val file = logFile ?: return null
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Onyx Debug Logs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Deletes the log file contents. A fresh file will be created on the next log write.
     */
    fun clearLogs() {
        synchronized(lock) {
            logFile?.takeIf { it.exists() }?.writeText("")
        }
        writeLine("INFO ", "Logs cleared by user")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun writeLine(level: String, message: String) {
        val file = logFile ?: return
        val timestamp = DATE_FMT.format(Date())
        val line = "$timestamp  $level  $message\n"

        synchronized(lock) {
            try {
                // Rotate if file is too large
                if (file.exists() && file.length() > MAX_SIZE_BYTES) {
                    rotate(file)
                }
                file.appendText(line, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log line", e)
            }
        }
    }

    /** Keep only the most recent [MAX_LINES] lines. */
    private fun rotate(file: File) {
        try {
            val lines = file.readLines(Charsets.UTF_8)
            if (lines.size > MAX_LINES) {
                val trimmed = lines.takeLast(MAX_LINES).joinToString("\n")
                file.writeText("$trimmed\n", Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Log rotation failed", e)
        }
    }

    /**
     * Installs a default uncaught exception handler that:
     * 1. Writes the full crash stack trace to the log file.
     * 2. Chains to the original handler so Android/Firebase Crashlytics still fire.
     */
    private fun installCrashHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeLine("FATAL", "══ UNCAUGHT EXCEPTION on thread '${thread.name}' ══")
                writeLine("FATAL", throwable.stackTraceToString())
                writeLine("FATAL", "═══════════════════════════════════════════════════")
            } catch (_: Exception) {
                // Must not throw — we are already in the crash path
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
