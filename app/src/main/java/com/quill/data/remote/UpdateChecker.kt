package com.quill.data.remote

import com.quill.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

@Singleton
class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val RELEASES_URL =
            "https://api.github.com/repos/ustc21xyx/quill/releases/latest"
    }

    suspend fun checkForUpdate(): UpdateInfo? = kotlinx.coroutines.withContext(
        kotlinx.coroutines.Dispatchers.IO
    ) {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val jsonObj = json.parseToJsonElement(body).jsonObject

            val tagName = jsonObj["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
            val remoteVersion = tagName.removePrefix("v")

            val releaseNotes = jsonObj["body"]?.jsonPrimitive?.content ?: ""

            val assets = jsonObj["assets"]?.jsonArray ?: return@withContext null
            val apkAsset = assets.firstOrNull { asset ->
                val name = asset.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                name.endsWith(".apk")
            } ?: return@withContext null

            val downloadUrl = apkAsset.jsonObject["browser_download_url"]
                ?.jsonPrimitive?.content ?: return@withContext null

            val currentVersion = BuildConfig.VERSION_NAME
            if (isNewerVersion(remoteVersion, currentVersion)) {
                UpdateInfo(
                    version = remoteVersion,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
