package com.ble.bledemo

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GattServiceAdapter(
    private val gattServices: List<GattServiceItem>,
    private val onCharacteristicClick: (BluetoothGattService,BluetoothGattCharacteristic) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SERVICE = 0
        private const val VIEW_TYPE_CHARACTERISTIC = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (gattServices[position].isExpanded) VIEW_TYPE_CHARACTERISTIC else VIEW_TYPE_SERVICE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SERVICE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gatt_service, parent, false)
            ServiceViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gatt_characteristic, parent, false)
            CharacteristicViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = gattServices[position]
        if (holder is ServiceViewHolder) {
            holder.bind(item.service,position)
            holder.itemView.setOnClickListener {
                // Toggle expansion
                item.isExpanded = !item.isExpanded
                notifyDataSetChanged() // Refresh the list to expand/collapse
//                onServiceClick(item.service)
            }
        } else if (holder is CharacteristicViewHolder) {
            holder.bind(item.characteristics,position)
            holder.itemView.setOnClickListener {
                Log.e("BLE","onCharacteristicClick : ${item.service.uuid} : ${item.characteristics[position].uuid}")
                onCharacteristicClick(item.service,item.characteristics[position])
            }
        }
    }

    override fun getItemCount(): Int = gattServices.size

    inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val serviceNameTextView: TextView = view.findViewById(R.id.serviceNameTextView)
        private val serviceUuidTextView: TextView = view.findViewById(R.id.serviceUuidTextView)

        fun bind(service: BluetoothGattService,position: Int) {
//            serviceNameTextView.text = if (service.isPrimary) "Primary Service" else "Secondary Service"
            serviceUuidTextView.text = "$position Service UUID: ${service.uuid} /n Characteristics:${service.characteristics.size}"
        }
    }

    inner class CharacteristicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val characteristicUuidTextView: TextView = view.findViewById(R.id.characteristicUuidTextView)

        fun bind(characteristics: List<BluetoothGattCharacteristic>,position: Int) {
            characteristicUuidTextView.text = "$position Characteristic UUID: ${characteristics[adapterPosition].uuid}"
        }
    }
}
