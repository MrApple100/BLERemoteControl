package com.mrapple100.bleremotecontrol

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ActivityViewModel(

): ViewModel() {
    val foundDevice = MutableLiveData<BluetoothDevice>();
    val statemotors = MutableLiveData<String>("OFF");


}