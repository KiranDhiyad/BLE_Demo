package com.ble.bledemo

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

data class GattServiceItem(
    val service: BluetoothGattService,
    val characteristics: List<BluetoothGattCharacteristic>,
    var isExpanded: Boolean = false
)