package com.ble.bledemo

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GattServiceAdapter(
    private val gattServices: List<GattServiceItem>,
    private val onCharacteristicClick: (BluetoothGattService,BluetoothGattCharacteristic) -> Unit
) : RecyclerView.Adapter<GattServiceAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gatt_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(gattServices[position],position)
    }

    override fun getItemCount(): Int = gattServices.size

    inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val serviceNameTextView: TextView = view.findViewById(R.id.serviceNameTextView)
        private val serviceUuidTextView: TextView = view.findViewById(R.id.serviceUuidTextView)
        private val characteristicsContainer: LinearLayout = view.findViewById(R.id.characteristicsContainer)

        fun bind(serviceItem: GattServiceItem,position: Int) {
//            serviceNameTextView.text = if (serviceItem.service.isPrimary) "Primary Service" else "Secondary Service"
            serviceNameTextView.text = "Service ${position+1} and it's Characteristics:${serviceItem.characteristics.size}"
            serviceUuidTextView.text = "UUID: " + serviceItem.service.uuid.toString()

            // Toggle characteristics visibility on click
            itemView.setOnClickListener {
                serviceItem.isExpanded = !serviceItem.isExpanded
                characteristicsContainer.visibility = if (serviceItem.isExpanded) View.VISIBLE else View.GONE
                if (serviceItem.isExpanded) showCharacteristics(serviceItem.service,serviceItem.characteristics)
            }
        }

        private fun showCharacteristics(serviceItem: BluetoothGattService,characteristics: List<BluetoothGattCharacteristic>) {
            Log.e("GattServiceAdapter", "showCharacteristics: $characteristics")
            characteristicsContainer.removeAllViews() // Clear any previous views

            for (characteristic in characteristics) {
                val characteristicView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_gatt_characteristic, characteristicsContainer, false)

                val uuidTextView: TextView = characteristicView.findViewById(R.id.characteristicUuidTextView)
                val propertiesTextView: TextView = characteristicView.findViewById(R.id.characteristicPropertiesTextView)
                val readDataTextView: TextView = characteristicView.findViewById(R.id.characteristicReadData)
                readDataTextView.visibility = View.GONE

                uuidTextView.text = "Characteristic UUID: " + characteristic.uuid.toString()
                propertiesTextView.text = "Characteristic Properties: " + getCharacteristicProperties(characteristic)
//                readDataTextView.text = "Read Data: " + characteristic.value.toString()

                characteristicView.setOnClickListener {
                    onCharacteristicClick(serviceItem,characteristic)
                }

                characteristicsContainer.addView(characteristicView)
                notifyDataSetChanged()

            }
        }

        private fun getCharacteristicProperties(characteristic: BluetoothGattCharacteristic): String {
            val properties = mutableListOf<String>()
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("Read")
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("Write")
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("Notify")
            return properties.joinToString(", ")
        }
    }

}
