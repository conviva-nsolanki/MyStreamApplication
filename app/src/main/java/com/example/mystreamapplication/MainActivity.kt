package com.example.mystreamapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.conviva.sdk.BuildConfig
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.example.mystreamapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val customerKeyDryRun = "1a6d7f0de15335c201e8e9aacbc7a0952f5191d7"

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initConvivaSDK()

        val fragment = ChooserFragment.newInstance()
        replaceFragment(fragment)

        mainViewModel.startVideo.observe(this) { playVideo ->
            if (playVideo) {
                val videoFragment = VideoFragment.newInstance()
                replaceFragment(videoFragment)
            }
        }
    }

    private fun initConvivaSDK() {
        // debug settings
        val gatewayUrl = " https://dryrun.testonly.conviva.com"
        println("nannandenden initialize conviva sdk")
        ConvivaAnalytics.init(this.applicationContext, customerKeyDryRun, mapOf(
            ConvivaSdkConstants.GATEWAY_URL to gatewayUrl,
            ConvivaSdkConstants.LOG_LEVEL to ConvivaSdkConstants.LogLevel.DEBUG
        ))

        ConvivaAnalytics.setDeviceInfo(mapOf(
            ConvivaSdkConstants.DEVICEINFO.DEVICE_TYPE to "Android",
            ConvivaSdkConstants.DEVICEINFO.DEVICE_VERSION to BuildConfig.VERSION
        ))

        VideoAnalytics.initialize(this.applicationContext)
    }

    private fun replaceFragment(fragment: Fragment) {
        this.supportFragmentManager.beginTransaction().replace(R.id.fl_container, fragment).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoAnalytics.release()
        ConvivaAnalytics.release()
    }
}