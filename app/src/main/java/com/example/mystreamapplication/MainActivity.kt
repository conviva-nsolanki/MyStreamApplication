package com.example.mystreamapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.example.mystreamapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = ChooserFragment.newInstance()
        replaceFragment(fragment)

        mainViewModel.startVideo.observe(this) { playVideo ->
            if (playVideo) {
                val videoFragment = VideoFragment.newInstance()
                replaceFragment(videoFragment)
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        this.supportFragmentManager.beginTransaction().replace(R.id.fl_container, fragment).commit()
    }
}