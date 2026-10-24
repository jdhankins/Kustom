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
 * Thread-safe logcat wrapper, in-memory log buffer, and crash file handler for Kustom.
 */
object KustomLogger {

    private const val TAG = "Kustom"
    private const val MAX_LOG_ENTRIES = 100
    private const val CRASH_LOG_FILE = "kustom_crash.log"

    private val logBuffer = CopyOnWriteArrayList<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var isCrashHandlerInstalled = false

    fun d(message: String) {
        Log.d(TAG, message)
        addBufferEntry("DEBUG", message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
        addBufferEntry("WARN", if (throwable != null) "$message\n${getStackTraceString(throwable)}" else message)
    }

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

    fun readCrashLog(context: Context): String {
        return runCatching {
            val crashFile = File(context.filesDir, CRASH_LOG_FILE)
            if (crashFile.exists()) crashFile.readText() else "No crash log found."
        }.getOrDefault("Error reading crash log.")
    }

    fun getBufferedLogs(): List<String> = logBuffer.toList()

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
