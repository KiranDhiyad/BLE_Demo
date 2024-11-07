package com.ble.bledemo

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService

data class DevicesModel(var bleDevice: BluetoothDevice? = null, var rssi: Int?, var isConnected: Boolean = false,
                        var gattServices: List<BluetoothGattService>? = null)
