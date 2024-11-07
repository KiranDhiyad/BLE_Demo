package com.ble.bledemo

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class DeviceAdapter(val context: Context, val listener: OnDeviceClickListener) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var devices = ArrayList<DevicesModel>()
    private var connectedDevicesMac: String = ""

    interface OnDeviceClickListener {
        fun onDeviceClick(device: BluetoothDevice)
        fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID)
        fun writeCharacteristic(serviceUUID: UUID, characteristicUUID: UUID, data: ByteArray)
        fun setCharacteristicNotification(serviceUUID: UUID, characteristicUUID: UUID, enable: Boolean)
    }

    fun addDevice(device: BluetoothDevice, rssi: Int) {
        Log.e("BLE", "addDevice :$device")
        devices.add(DevicesModel(bleDevice = device,rssi = rssi))
        notifyDataSetChanged()
    }

    fun deviceStatus(deviceMac: String) {
        connectedDevicesMac = deviceMac
        Log.e("BLE", "deviceConnected :$connectedDevicesMac")

        notifyDataSetChanged()
    }

    fun deviceServices(macAddress: String, gattServices: List<BluetoothGattService>){
        Log.e("BLE", "deviceServices :$macAddress gattServices size: ${gattServices.size}")
        devices.find { it.bleDevice?.address == macAddress }?.let { device ->
            device.gattServices = gattServices
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.devices_list_sample, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceAdapter.ViewHolder, position: Int) {
        val device = devices[position]

        holder.deviceName.text = device.bleDevice?.name ?: "Unknown Device"

        if (device.bleDevice?.address == connectedDevicesMac){
            holder.deviceAddress.text = "MAC: ${device.bleDevice?.address}, RSSI: ${device.rssi}  (Connected)"
        } else {
            holder.deviceAddress.text = "MAC: ${device.bleDevice?.address}, RSSI: ${device.rssi}"
        }


        if (device.gattServices != null) {
            holder.tvServices.text = "Services: ${device.gattServices?.size}"
            holder.gattServiceRecyclerView.layoutManager = LinearLayoutManager(context)
            val gattServices = device.gattServices?.map { service ->
                GattServiceItem(service, service.characteristics)
            }
            // Initialize the adapter
            val gattServiceAdapter = GattServiceAdapter(gattServices!!) { service,characteristic ->
                listener.readCharacteristic(service.uuid, characteristic.uuid)
            }
            holder.gattServiceRecyclerView.adapter = gattServiceAdapter
        }else{
            holder.tvServices.text = "Services: 0"
        }

        // Set click listener for each item
        holder.itemView.setOnClickListener {
            device.bleDevice?.let { it1 -> listener.onDeviceClick(it1) }  // Notify the listener (MainActivity) of the click
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.tvText)
        val deviceAddress: TextView = itemView.findViewById(R.id.tvText2)
        val tvServices: TextView = itemView.findViewById(R.id.tvServices)
        val gattServiceRecyclerView: RecyclerView = itemView.findViewById(R.id.gattServiceRecyclerView)
    }
}