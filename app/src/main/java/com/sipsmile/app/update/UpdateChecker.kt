package com.sipsmile.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val apkUrl: String, val pageUrl: String, val notes: String)

/**
 * Asks GitHub for the newest release of the app. Android doesn't allow silent updates for apps that
 * are installed outside the Play Store, so the app only tells you and opens the download.
 * Works while the repository is Public.
 */
object UpdateChecker {
    private const val REPO = "Nandani-10/Sip-Smile-App"

    fun currentVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    } catch (e: Exception) {
        "0"
    }

    /** "v1.2.0" / "1.2" -> [1, 2, 0]. Anything unreadable counts as 0. */
    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .split('.')
            .map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

    fun isNewer(latest: String, current: String): Boolean {
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** The latest release, or null when it can't be reached (offline, private repo, no release yet). */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name")
            val assets = json.optJSONArray("assets")
            var apk = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val url = assets.getJSONObject(i).optString("browser_download_url")
                    if (url.endsWith(".apk")) {
                        apk = url
                        break
                    }
                }
            }
            if (tag.isBlank() || apk.isBlank()) null
            else UpdateInfo(tag.removePrefix("v"), apk, json.optString("html_url"), json.optString("body"))
        } catch (e: Exception) {
            null
        }
    }

    /** Opens the APK link in the browser, which downloads it; tapping the download installs the update. */
    fun openDownload(context: Context, info: UpdateInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
