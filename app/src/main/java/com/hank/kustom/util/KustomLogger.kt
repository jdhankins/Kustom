package com.hank.kustom.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe logcat wrapper, in-memory log buffer, and crash log file persistence utility.
 */
object KustomLogger {

    private const val TAG = "Kustom"
    private const val MAX_LOG_ENTRIES = 100
    private const val CRASH_LOG_FILE = "kustom_crash.log"

    private val logBuffer = CopyOnWriteArrayList<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var isCrashHandlerInstalled = false

    /**
     * Logs a debug level message.
     *
     * @param message Debug log text.
     */
    fun d(message: String) {
        Log.d(TAG, message)
        addBufferEntry("DEBUG", message)
    }

    /**
     * Logs a warning level message.
     *
     * @param message Warning log text.
     * @param throwable Optional exception or error instance.
     */
    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
        addBufferEntry("WARN", if (throwable != null) "$message\n${getStackTraceString(throwable)}" else message)
    }

    /**
     * Logs an error level message.
     *
     * @param message Error log text.
     * @param throwable Optional exception or error instance.
     */
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        val fullMsg = if (throwable != null) "$message\n${getStackTraceString(throwable)}" else message
        addBufferEntry("ERROR", fullMsg)
    }

    private fun addBufferEntry(level: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] [$level] $message"
        logBuffer.add(entry)
        while (logBuffer.size > MAX_LOG_ENTRIES) {
            if (logBuffer.isNotEmpty()) {
                logBuffer.removeAt(0)
            }
        }
    }

    /**
     * Installs an uncaught exception crash handler that persists crash stack traces to disk.
     *
     * @param context Application or activity context.
     */
    fun installCrashHandler(context: Context) {
        if (isCrashHandlerInstalled) return
        isCrashHandlerInstalled = true

        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = getStackTraceString(throwable)
            e("Uncaught exception in thread ${thread.name}", throwable)
            saveCrashToFile(appContext, thread.name, stackTrace)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashToFile(context: Context, threadName: String, stackTrace: String) {
        runCatching {
            val crashFile = File(context.filesDir, CRASH_LOG_FILE)
            val timestamp = dateFormat.format(Date())
            val crashData = """
                ========================================
                CRASH TIMESTAMP: $timestamp
                THREAD: $threadName
                $stackTrace
                ========================================
                
            """.trimIndent()
            crashFile.appendText(crashData)
        }
    }

    /**
     * Reads saved crash logs from disk.
     *
     * @param context Application or activity context.
     * @return String containing saved crash log text, or a notice if empty.
     */
    fun readCrashLog(context: Context): String {
        return runCatching {
            val crashFile = File(context.filesDir, CRASH_LOG_FILE)
            if (crashFile.exists()) crashFile.readText() else "No crash log found."
        }.getOrDefault("Error reading crash log.")
    }

    /**
     * Returns a copy of the in-memory buffered log entries.
     *
     * @return List of formatted log entry strings.
     */
    fun getBufferedLogs(): List<String> = logBuffer.toList()

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
