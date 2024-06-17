package com.example.mystreamapplication

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.conviva.apptracker.ConvivaAppAnalytics
import com.conviva.sdk.BuildConfig
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.example.mystreamapplication.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    // DryRun
    private val customerKeyDryRun = ""

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initConvivaSDK()

        val fragment = ChooserFragment.newInstance()
        fragmentTransaction(fragment)

        mainViewModel.playVideo.observe(this) { playVideo ->
            if (!playVideo.isNullOrEmpty()) {
                val videoFragment = VideoFragment.newInstance(playVideo)
                fragmentTransaction(videoFragment, replace = false)
            }
        }
    }

    private fun initConvivaSDK() {
        // debug settings
        val gatewayUrl = " https://dryrun.testonly.conviva.com"
        println("nannandenden initialize conviva sdk")
        ConvivaAnalytics.init(this, customerKeyDryRun, mapOf(
            ConvivaSdkConstants.GATEWAY_URL to gatewayUrl,
            ConvivaSdkConstants.LOG_LEVEL to ConvivaSdkConstants.LogLevel.DEBUG
        ))

        ConvivaAppAnalytics.createTracker(
            this,
            customerKeyDryRun,
            "Android Video"
        )?.also {
            ConvivaAppAnalytics.setTrackerAsDefault(it)
        }

    }

    private fun fragmentTransaction(fragment: Fragment, replace: Boolean = true) {
        val transaction = this.supportFragmentManager.beginTransaction()
        if (replace) {
            transaction.replace(R.id.fl_container, fragment)
        } else {
            transaction.add(R.id.fl_container, fragment)
        }
        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoAnalytics.release()
        ConvivaAnalytics.release()
    }
}