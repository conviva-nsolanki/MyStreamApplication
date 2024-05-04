package com.example.mystreamapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mystreamapplication.VideoFragment.Companion.VIDEO_CSAI
import com.example.mystreamapplication.VideoFragment.Companion.VIDEO_NO_ADS
import com.example.mystreamapplication.VideoFragment.Companion.VIDEO_SSAI

class MainViewModel : ViewModel() {

    private val _setVideos = MutableLiveData(listOf<String>())
    val setVideos: LiveData<List<String>> = _setVideos

    private val _playVideo = MutableLiveData<String>("")
    val playVideo: LiveData<String> = _playVideo

    init {
        _setVideos.value = listOf(VIDEO_NO_ADS, VIDEO_CSAI, VIDEO_SSAI)
    }


    fun onPlayVideoSelected(position: Int) {
        _playVideo.value = _setVideos.value?.get(position)?: ""
    }


}