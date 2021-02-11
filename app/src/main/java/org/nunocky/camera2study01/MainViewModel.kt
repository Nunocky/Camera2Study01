package org.nunocky.camera2study01

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val scaleX = MutableLiveData(1)
    val scaleY = MutableLiveData(1)
    val rotation = MutableLiveData(0)
}