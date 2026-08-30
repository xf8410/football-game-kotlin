package com.football.game

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 崩溃捕获：
 * - 启动时安装默认 UncaughtExceptionHandler，崩溃时把完整堆栈写到外部私有目录
 *   （/sdcard/Android/data/com.football.game/files/last_crash.txt）
 * - 下次启动 MainActivity 读到该文件 → 用"原生 View"显示崩溃报告页（不依赖 Compose，
 *   保证 UI 框架自身崩溃时也能显示），用户截图发回即可精确定位
 */
object CrashReporter {

    fun crashFile(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, "last_crash.txt")
    }

    fun readPreviousCrash(context: Context): String? {
        val f = crashFile(context)
        return if (f.exists() && f.length() > 0L) {
            try {
                f.readText()
            } catch (t: Throwable) {
                null
            }
        } else {
            null
        }
    }

    fun clear(context: Context) {
        try {
            crashFile(context).delete()
        } catch (_: Throwable) {
        }
    }

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val text = buildString {
                    appendLine("=== 足球游戏崩溃报告 ===")
                    appendLine("thread: ${thread.name}")
                    appendLine("time: ${System.currentTimeMillis()}")
                    appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine()
                    append(sw.toString())
                    val cause = throwable.cause
                    if (cause != null && cause !== throwable) {
                        appendLine()
                        appendLine("=== CAUSE ===")
                        val sw2 = StringWriter()
                        cause.printStackTrace(PrintWriter(sw2))
                        append(sw2.toString())
                    }
                }
                crashFile(context).writeText(text)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
