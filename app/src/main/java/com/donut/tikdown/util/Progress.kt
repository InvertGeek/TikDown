package com.donut.tikdown.util

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

class ProgressContent(
    private var tip: String = "下载中"
) {
    private var progress: Float by mutableFloatStateOf(0f)
    private var bytesWritten: Long by mutableLongStateOf(0)
    private var contentLength: Long by mutableLongStateOf(0)

    val ktorListener: suspend (bytesWritten: Long, bytesTotal: Long?) -> Unit = { bytes, length ->
        updateProgress(bytes, length ?: 1)
    }

    fun updateProgress(written: Long = bytesWritten, total: Long = contentLength) {
        bytesWritten = written
        contentLength = total.coerceAtLeast(1)
        progress = bytesWritten.toFloat() / contentLength.toFloat()
    }

    @Composable
    fun LoadingContent(show: Boolean = true) {
        LoadingBar(
            progress = progress,
            bytesWritten,
            contentLength,
            tip = tip,
            show
        )
    }

    fun increaseBytesWritten(bytes: Long, total: Long) {
        bytesWritten += bytes
        contentLength = total.coerceAtLeast(1)
        updateProgress()
    }

}

@Composable
fun LoadingBar(
    progress: Float,
    bytesWritten: Long,
    contentLength: Long,
    tip: String,
    show: Boolean = true
) {

    val sizeDp by animateDpAsState(if (show) 600.dp else 0.dp, label = "progress")

    Column(
        modifier = Modifier
            .heightIn(0.dp, sizeDp)
            .alpha(if (show) 1f else 0f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (progress <= 0) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "${tip}: ${formatFileSize(bytesWritten)}/${formatFileSize(contentLength)}")
            }
        }
    }
}


fun interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}