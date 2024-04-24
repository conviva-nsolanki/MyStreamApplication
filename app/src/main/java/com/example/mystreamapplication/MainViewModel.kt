package com.example.mystreamapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    private val _startVide = MutableLiveData(false)
    val startVideo: LiveData<Boolean> = _startVide

    fun onStartVideSelected() {
        _startVide.value = true
    }



}