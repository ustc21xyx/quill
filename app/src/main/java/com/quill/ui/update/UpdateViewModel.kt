package com.quill.ui.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.remote.UpdateChecker
import com.quill.data.remote.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val okHttpClient: OkHttpClient,
) : ViewModel() {

    private val _updateAvailable = MutableStateFlow<UpdateInfo?>(null)
    val updateAvailable: StateFlow<UpdateInfo?> = _updateAvailable.asStateFlow()

    private val _downloadProgress = MutableStateFlow(-1f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            _updateAvailable.value = updateChecker.checkForUpdate()
        }
    }

    fun dismiss() {
        _updateAvailable.value = null
        cancelDownload()
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadProgress.value = -1f
    }

    fun downloadAndInstall(context: Context) {
        val info = _updateAvailable.value ?: return
        if (downloadJob?.isActive == true) return

        downloadJob = viewModelScope.launch {
            _downloadProgress.value = 0f
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    downloadApk(context, info.downloadUrl)
                }
                if (apkFile != null) {
                    installApk(context, apkFile)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                _downloadProgress.value = -1f
            } catch (_: Exception) {
                _downloadProgress.value = -1f
            }
        }
    }

    private fun downloadApk(context: Context, url: String): File? {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val apkFile = File(updatesDir, "quill-update.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            _downloadProgress.value = -1f
            return null
        }

        val body = response.body ?: run {
            _downloadProgress.value = -1f
            return null
        }
        val totalBytes = body.contentLength()

        body.byteStream().use { input ->
            apkFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    if (totalBytes > 0) {
                        _downloadProgress.value = bytesRead.toFloat() / totalBytes.toFloat()
                    }
                }
            }
        }

        _downloadProgress.value = 1f
        return apkFile
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
