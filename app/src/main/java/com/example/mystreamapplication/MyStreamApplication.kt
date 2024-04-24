package com.example.mystreamapplication

import android.app.Application
import android.provider.MediaStore.Video
import com.conviva.sdk.BuildConfig
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants

class MyStreamApplication: Application() {

    private val customerKey = "1a6d7f0de15335c201e8e9aacbc7a0952f5191d7"

    override fun onCreate() {
        super.onCreate()
        // debug settings
        val gatewayUrl = " https://dryrun.testonly.conviva.com"
        println("nannandenden initialize conviva sdk")
        ConvivaAnalytics.init(this, customerKey, mapOf(
            ConvivaSdkConstants.GATEWAY_URL to gatewayUrl,
            ConvivaSdkConstants.LOG_LEVEL to ConvivaSdkConstants.LogLevel.DEBUG
        ))

        ConvivaAnalytics.setDeviceInfo(mapOf(
            ConvivaSdkConstants.DEVICEINFO.DEVICE_TYPE to "Android",
            ConvivaSdkConstants.DEVICEINFO.DEVICE_VERSION to BuildConfig.VERSION
        ))

        VideoAnalytics.initialize(this)

    }
}