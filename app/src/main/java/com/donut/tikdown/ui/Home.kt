package com.donut.tikdown.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.donut.tikdown.currentActivity
import com.donut.tikdown.ui.component.common.MixDialogBuilder
import com.donut.tikdown.ui.theme.colorScheme
import com.donut.tikdown.util.isTrue
import com.donut.tikdown.util.readClipBoardText

@Composable
fun Home() {
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
                        currentActivity?.startActivity(intent)
                    }
                    show()
                }
            }
    )
    resultContent()
}