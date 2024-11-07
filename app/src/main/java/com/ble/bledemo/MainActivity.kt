package com.ble.bledemo

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class MainActivity : AppCompatActivity(), DeviceAdapter.OnDeviceClickListener {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private lateinit var scan: Button
    private lateinit var deviceRecyclerView: RecyclerView

    private val scanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var bluetoothGatt: BluetoothGatt? = null

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    private val REQUEST_CODE_PERMISSIONS = 10
    private val SCAN_PERIOD: Long = 10000 // Scan for 10 seconds
    private var isScanning = false
    // Set to store unique device addresses
    private val discoveredDevices = mutableSetOf<String>()
    private var selectedDevice: BluetoothDevice? = null
    private var deviceAdapter: DeviceAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        scan = findViewById(R.id.scanButton)
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)

        deviceAdapter = DeviceAdapter(this,this);
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceRecyclerView.adapter = deviceAdapter

        checkBluetoothSupport()
        requestPermissions()

        scan.setOnClickListener { requestPermissions() }
    }

    private fun log (message: String) {
        Log.e("BLE", message)
    }

    // check bluetooth support
    private fun isBleSupported(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    private fun checkBluetoothSupport() {
        if (bluetoothAdapter == null || !isBleSupported()) {
            AlertDialog.Builder(this)
                .setTitle("BLE Not Supported")
                .setMessage("This device does not support Bluetooth Low Energy")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
        }
    }

    // permission handle
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (!allPermissionsGranted()) {
            log("All permissions not granted")
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            log("All permissions granted startScanning")
            // Permissions are already granted, start BLE operations
            startScanning()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, start BLE scanning
                log("onRequestPermissionsResult All permissions granted startScanning")
                startScanning()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // scan ble and handling
    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (bluetoothAdapter?.isEnabled == false) {
            log("startScanning bluetoothAdapter isEnabled == false")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 100)
        }else{
            if (!isScanning) {
                isScanning = true
                scanner?.startScan(scanCallback)
                Toast.makeText(this, "Scanning started...", Toast.LENGTH_SHORT).show()

                // Stop scanning after the specified SCAN_PERIOD
                Handler(Looper.getMainLooper()).postDelayed({
                    stopScanning()
                }, SCAN_PERIOD)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                startScanning()  // Start scanning if Bluetooth is now enabled
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address

            // Check if the device is already in the set
            if (deviceAddress !in discoveredDevices) {
                // Add the new device to the set and RecyclerView
                discoveredDevices.add(deviceAddress)
                deviceAdapter?.addDevice(result.device, result.rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            Toast.makeText(this@MainActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopScanning() {
        if (isScanning) {
            isScanning = false
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            scanner?.stopScan(scanCallback)
            log("Scanning stopped")
            Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show()
        }
    }

    // connect to device
    fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    // Function to disconnect from a BLE device
    fun disconnectDevice() {
        bluetoothGatt?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            it.disconnect()  // Initiates disconnection
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    runOnUiThread { Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show() }
                    log("Device Connected")
                    bluetoothGatt?.discoverServices()
                    selectedDevice?.address?.let {
                        deviceRecyclerView.smoothScrollToPosition(0)
                        deviceAdapter?.deviceStatus(it)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("Device Disconnected")
                    deviceAdapter?.deviceStatus("")
                    deviceRecyclerView.smoothScrollToPosition(0)
                    selectedDevice = null
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    runOnUiThread { Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("GattCallback onServicesDiscovered received: $status")
                displayGattServices(gatt.services)  // Display or work with services
            } else {
                log("GattCallback onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value  // Get the data from characteristic
                processCharacteristicData(data)
            }
            log("GattCallback onCharacteristicRead received: $status")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value  // Data from notification
            processCharacteristicData(data)
            log("GattCallback onCharacteristicChanged received: $data")
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        for (gattService in gattServices) {
            log("BLEService Service UUID: ${gattService.uuid} : Type : ${gattService.type} ")

            // List all characteristics for each service
            for (gattCharacteristic in gattService.characteristics) {
                log("BLECharacteristic Characteristic UUID: ${gattCharacteristic.uuid} properties: ${gattCharacteristic.properties}")
            }
        }
        deviceAdapter?.deviceServices(selectedDevice?.address!!,gattServices)
        deviceRecyclerView.smoothScrollToPosition(0)
    }

    private fun processCharacteristicData(data: ByteArray?) {
        data?.let {
            val stringData = it.toString(Charsets.UTF_8) // Convert byte array to string, if applicable
            Log.e("CharacteristicData", "Received data: $stringData")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Get Data: $stringData", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onDeviceClick(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        stopScanning()
        if (selectedDevice == device){
            if (bluetoothGatt == null){
                log("Connecting to device: ${device.name ?: "Unknown"}")
                Toast.makeText(this, "Connecting to device: ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
                connectToDevice(device)
            }else{
                log("Disconnecting from device : ${device.name ?: "Unknown"}")
                Toast.makeText(this, "Disconnecting from device : ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
                disconnectDevice()
            }
        }else{
            log("Connecting to device: ${device.name ?: "Unknown"}")
            Toast.makeText(this, "Connecting to device: ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
            selectedDevice = device
            connectToDevice(device)
        }
    }

    override fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID) {
        log("readCharacteristic called serviceUUID: $serviceUUID characteristicUUID: $characteristicUUID")
        val service = bluetoothGatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)
        if (characteristic != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            runOnUiThread {
                Toast.makeText(this, "Read Characteristic", Toast.LENGTH_SHORT).show()
            }
            bluetoothGatt?.readCharacteristic(characteristic)
        }
    }

    override fun writeCharacteristic(serviceUUID: UUID, characteristicUUID: UUID, data: ByteArray) {
        val service = bluetoothGatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)
        characteristic?.let {
            it.value = data
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    override fun setCharacteristicNotification(serviceUUID: UUID, characteristicUUID: UUID, enabled: Boolean) {
        val service = bluetoothGatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothGatt?.setCharacteristicNotification(characteristic, enabled)

        // Some characteristics require setting a descriptor for enabling notifications
        val descriptor = characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.value = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(descriptor)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }
}