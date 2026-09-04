package com.football.game.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub 仓库直传（App 内完成）
 * - 上传自定义启动图：选图 → 压缩 → PUT 到 app/src/main/assets/splash.jpg
 * - 触发 CI 构建：POST workflow_dispatch → Releases 出新版安装包
 *
 * 使用用户自己的 Personal Access Token（需勾选 repo + workflow 权限），
 * Token 仅保存在手机本地 SharedPreferences，不上传到任何第三方。
 */
object GitHubUploader {

    private const val OWNER = "xf8410"
    private const val REPO = "football-game-kotlin"
    private const val BRANCH = "workbench/gk-sub-position-choice"
    private const val ASSET_PATH = "app/src/main/assets/splash.jpg"
    private const val API = "https://api.github.com"

    /** 一键直达的 Token 生成页（已预选 repo + workflow 权限） */
    const val TOKEN_URL =
        "https://github.com/settings/tokens/new?scopes=repo,workflow&description=FootballGame"

    data class Result(val ok: Boolean, val message: String)

    private class ShaResp(val sha: String? = null)

    /** 选图后处理：限制最大宽度 1600px、JPEG 90 压缩（统一转成 splash.jpg） */
    fun prepareImageBytes(context: Context, uri: Uri, maxWidth: Int = 1600): ByteArray? = try {
        val src = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null
        val scaled = if (src.width > maxWidth) {
            val h = (src.height.toLong() * maxWidth / src.width).toInt()
            Bitmap.createScaledBitmap(src, maxWidth, h, true)
        } else {
            src
        }
        ByteArrayOutputStream()
            .also { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            .toByteArray()
    } catch (_: Throwable) {
        null
    }

    /**
     * 上传启动图（覆盖 app/src/main/assets/splash.jpg；已存在时自动携带 sha 走更新）
     */
    suspend fun uploadSplashImage(token: String, imageBytes: ByteArray): Result =
        withContext(Dispatchers.IO) {
            try {
                val sha = fetchSha(token)
                val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val body = StringBuilder().apply {
                    append("{")
                    append("\"message\":\"自定义启动图（App 内上传）\",")
                    sha?.let { append("\"sha\":\"$it\",") }
                    append("\"content\":\"$b64\",")
                    append("\"branch\":\"$BRANCH\"")
                    append("}")
                }.toString()

                val conn = open("$API/repos/$OWNER/$REPO/contents/$ASSET_PATH", token)
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val err = if (code in 200..299) "" else readErr(conn)
                conn.disconnect()

                if (code in 200..299) {
                    Result(true, "✅ 上传成功！点\"构建新版 APK\"，约 6 分钟后到 Releases 下载新版")
                } else {
                    Result(false, "上传失败 (HTTP $code)：${err.take(200)}")
                }
            } catch (e: Throwable) {
                Result(false, "上传异常：${e.message}")
            }
        }

    /**
     * 触发 CI 构建（Token 需包含 workflow 权限）
     */
    suspend fun triggerBuild(token: String): Result = withContext(Dispatchers.IO) {
        try {
            val conn = open("$API/repos/$OWNER/$REPO/actions/workflows/build.yml/dispatches", token)
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write("{\"ref\":\"$BRANCH\"}".toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                Result(true, "✅ 构建已触发！约 6 分钟后到 Releases 下载新版安装包")
            } else {
                Result(false, "触发失败 (HTTP $code)：Token 需要勾选 workflow 权限")
            }
        } catch (e: Throwable) {
            Result(false, "触发异常：${e.message}")
        }
    }

    /** 校验 Token 是否有效（能否读取仓库） */
    suspend fun verifyToken(token: String): Result = withContext(Dispatchers.IO) {
        try {
            val conn = open("$API/repos/$OWNER/$REPO", token)
            val code = conn.responseCode
            conn.disconnect()
            if (code == 200) {
                Result(true, "✅ Token 有效，可以使用了")
            } else {
                Result(false, "Token 无效 (HTTP $code)，请检查是否复制完整、权限是否勾选")
            }
        } catch (e: Throwable) {
            Result(false, "校验异常：${e.message}")
        }
    }

    private fun open(url: String, token: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return conn
    }

    private fun fetchSha(token: String): String? = try {
        val conn = open("$API/repos/$OWNER/$REPO/contents/$ASSET_PATH?ref=$BRANCH", token)
        val code = conn.responseCode
        val text = if (code == 200) conn.inputStream.bufferedReader().readText() else null
        conn.disconnect()
        text?.let { Gson().fromJson(it, ShaResp::class.java)?.sha }
    } catch (_: Throwable) {
        null
    }

    private fun readErr(conn: HttpURLConnection): String = try {
        conn.errorStream?.bufferedReader()?.readText() ?: ""
    } catch (_: Throwable) {
        ""
    }
}
