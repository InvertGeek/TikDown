package com.donut.tikdown

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.donut.tikdown.ui.MainContent
import com.donut.tikdown.ui.fetchVideo
import com.donut.tikdown.ui.videoUrl
import com.donut.tikdown.util.objects.MixActivity
import com.donut.tikdown.util.readClipBoardText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : MixActivity("main") {
    override fun onResume() {
        super.onResume()
        appScope.launch {
            delay(100)
            val clipboardText = readClipBoardText()
            if (clipboardText.isEmpty() || clipboardText.contentEquals(videoUrl)) {
                return@launch
            }
            var videoUrl = clipboardText
            fetchVideo(videoUrl)
        }
    }


    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainContent()
        }
    }
}






