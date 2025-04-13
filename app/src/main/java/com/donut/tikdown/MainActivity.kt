package com.donut.tikdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.donut.tikdown.ui.component.common.MixDialogBuilder
import com.donut.tikdown.ui.theme.MainTheme
import com.donut.tikdown.ui.theme.colorScheme
import com.donut.tikdown.util.ProgressContent
import com.donut.tikdown.util.UseEffect
import com.donut.tikdown.util.client
import com.donut.tikdown.util.copyToClipboard
import com.donut.tikdown.util.extractUrls
import com.donut.tikdown.util.formatFileSize
import com.donut.tikdown.util.genRandomString
import com.donut.tikdown.util.getVideoId
import com.donut.tikdown.util.ignoreError
import com.donut.tikdown.util.isTrue
import com.donut.tikdown.util.objects.MixActivity
import com.donut.tikdown.util.readClipBoardText
import com.donut.tikdown.util.saveFileToStorage
import com.donut.tikdown.util.showError
import com.donut.tikdown.util.showToast
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.url
import io.ktor.http.contentLength
import io.sanghun.compose.video.RepeatMode
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : MixActivity("main") {

    var resultContent: @Composable () -> Unit by mutableStateOf(
        {}
    )

    private var videoUrl by mutableStateOf("")

    override fun onResume() {
        super.onResume()

        appScope.launch {
            delay(100)
            val clipboardText = readClipBoardText()
            if (clipboardText.isEmpty() || clipboardText.contentEquals(videoUrl)) {
                return@launch
            }
            videoUrl = clipboardText
            fetchVideo(videoUrl)
        }
    }


    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                Column(
                    modifier = Modifier
                        .systemBarsPadding()
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "抖音视频解析",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = {
                            videoUrl = it
                        },
                        trailingIcon = {
                            videoUrl.isNotEmpty().isTrue {
                                Icon(
                                    Icons.Outlined.Close,
                                    tint = colorScheme.primary,
                                    contentDescription = "clear",

                                    modifier = Modifier.clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) {
                                        videoUrl = ""
                                    })
                            }
                        },
                        label = {
                            Text(text = "输入分享地址")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                videoUrl = readClipBoardText()
                                fetchVideo(videoUrl)
                            }, modifier = Modifier
                                .weight(1.0f)
                                .padding(10.dp, 0.dp)
                        ) {
                            Text(text = "粘贴地址")
                        }
                        Button(
                            onClick = {
                                fetchVideo(videoUrl)
                            }, modifier = Modifier
                                .weight(1.0f)
                                .padding(10.dp, 0.dp)
                        ) {
                            Text(text = "解析")
                        }
                    }
                    Text(
                        text = "https://github.com/invertgeek/TikDown",
                        color = colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                MixDialogBuilder("打开链接?").apply {
                                    setDefaultNegative()
                                    setPositiveButton("打开") {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://github.com/invertgeek/TikDown".toUri()
                                        )
                                        closeDialog()
                                        startActivity(intent)
                                    }
                                    show()
                                }
                            }
                    )
                    resultContent()
                }
            }
        }
    }

    @Composable
    fun VideoContent(videoUrl: String) {
        VideoPlayer(
            mediaItems = listOf(
                VideoPlayerMediaItem.NetworkMediaItem(videoUrl),
            ),
            controllerConfig = VideoPlayerControllerConfig(
                showSpeedAndPitchOverlay = false,
                showSubtitleButton = false,
                showCurrentTimeAndTotalTime = true,
                showBufferingProgress = false,
                showForwardIncrementButton = false,
                showBackwardIncrementButton = false,
                showBackTrackButton = false,
                showNextTrackButton = false,
                showRepeatModeButton = false,
                controllerShowTimeMilliSeconds = 5_000,
                controllerAutoShow = true,
                showFullScreenButton = false
            ),
            handleLifecycle = false,
            autoPlay = true,
            usePlayerController = true,
            enablePip = false,
            handleAudioFocus = true,
            repeatMode = RepeatMode.ALL,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .heightIn(600.dp)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun selectVideoName(): String =
        suspendCancellableCoroutine { task ->
            MixDialogBuilder("文件名称").apply {

                var videoName by mutableStateOf("抖音下载 ${genRandomString(4)}")

                if (videoUrl.contains("#")) {
                    videoName = videoUrl.substringAfter("#").substringBefore("#").trim()
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
            properties = DialogProperties(dismissOnClickOutside = false)
        ).apply {
            setContent {
                val progress = remember {
                    ProgressContent()
                }
                UseEffect {
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
            "解析中", properties = DialogProperties(
                dismissOnClickOutside = false
            )
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


}

