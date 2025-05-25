package com.donut.tikdown.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.tikdown.ui.component.common.MixDialogBuilder
import com.donut.tikdown.ui.theme.MainTheme
import com.donut.tikdown.ui.theme.colorScheme
import com.donut.tikdown.util.AsyncEffect
import com.donut.tikdown.util.ProgressContent
import com.donut.tikdown.util.client
import com.donut.tikdown.util.copyToClipboard
import com.donut.tikdown.util.extractUrls
import com.donut.tikdown.util.formatFileSize
import com.donut.tikdown.util.genRandomString
import com.donut.tikdown.util.getVideoId
import com.donut.tikdown.util.ignoreError
import com.donut.tikdown.util.saveFileToStorage
import com.donut.tikdown.util.showError
import com.donut.tikdown.util.showToast
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.url
import io.ktor.http.contentLength
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

var resultContent: @Composable () -> Unit by mutableStateOf(
    {}
)

var videoUrl by mutableStateOf("")

@Composable
fun MainContent() {
    MainTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Home()
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun selectVideoName(): String =
    suspendCancellableCoroutine { task ->
        MixDialogBuilder("文件名称").apply {

            var videoName by mutableStateOf("抖音下载 ${genRandomString(4)}")

            if (videoUrl.contains("#")) {
                videoUrl.substringAfter("#").substringBefore("#").trim().let {
                    if (it.isNotEmpty()) {
                        videoName = it
                    }
                }
            }
            setContent {
                OutlinedTextField(value = videoName, onValueChange = {
                    videoName = it
                }, modifier = Modifier.fillMaxWidth(), label = {
                    Text(text = "请输入文件名称")
                })
            }
            setDefaultNegative()
            setPositiveButton("确认") {
                if (videoName.trim().isEmpty()) {
                    showToast("文件名称不能为空")
                    return@setPositiveButton
                }
                task.resume(videoName)
                closeDialog()
            }
            show()
        }
    }


private suspend fun saveVideo(videoUrl: String) {
    val name = selectVideoName()
    MixDialogBuilder(
        "下载中",
        autoClose = false
    ).apply {
        setContent {
            val progress = remember {
                ProgressContent()
            }
            AsyncEffect {
                saveFileToStorage(videoUrl, "${name}.mp4", progress)
                showToast("文件已保存到下载目录")
                closeDialog()
            }
            progress.LoadingContent()
        }
        setNegativeButton("取消") {
            closeDialog()
            showToast("下载已取消")
        }
        show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
fun showVideoInfo(id: String, size: Long = 0) {
    val videoUrl = "https://www.douyin.com/aweme/v1/play/?video_id=${id}"

    @Composable
    fun InfoText(key: String, value: String) {
        FlowRow {
            Text(text = key)
            Text(
                text = value,
                color = colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    value.copyToClipboard()
                })
        }
    }
    resultContent = {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(0.dp, 10.dp)
        ) {
            Text(text = "视频信息: ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoText(key = "视频id: ", value = id)
            InfoText(key = "大小: ", value = formatFileSize(size))
            InfoText(
                key = "永久直链播放地址: ",
                value = videoUrl
            )
            val scope = rememberCoroutineScope()
            OutlinedButton(onClick = {
                scope.launch {
                    saveVideo(videoUrl)
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "下载视频")
            }
            VideoContent(videoUrl = videoUrl)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
fun fetchVideo(videoUrl: String) {
    val url = extractUrls(videoUrl).lastOrNull()
    if (url == null) {
        showToast("请输入正确的分享链接")
        return
    }
    MixDialogBuilder(
        "解析中",
        autoClose = false
    ).apply {
        setContent {
            LaunchedEffect(Unit) {
                try {
                    val videoId = getVideoId(url)
                    val videoPlayUrl =
                        "https://www.douyin.com/aweme/v1/play/?video_id=${videoId}"
                    val playResponse = client.config {
                        followRedirects = false
                    }.get {
                        url(videoPlayUrl)
                    }
                    if (playResponse.status.value != 302) {
                        showToast("不支持广告或无法播放")
                        return@LaunchedEffect
                    }
                    var size = 0L
                    ignoreError {
                        val videoSizeResponse = client.head {
                            url(videoPlayUrl)
                        }
                        size = videoSizeResponse.contentLength() ?: size
                    }
                    showToast("解析成功!")
                    showVideoInfo(videoId, size)
                } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) {
                        return@LaunchedEffect
                    }
                    showError(e)
                    when (e.message) {
                        "不支持图文",
                        "不支持分段视频",
                        "作品已失效",
                            -> {
                            showToast("解析失败(${e.message})")
                        }

                        else -> {
                            showToast("解析失败(${e.message}),重试中")
                            fetchVideo(videoUrl)
                        }
                    }

                } finally {
                    closeDialog()
                }
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        setDefaultNegative()
        show()
    }
}

