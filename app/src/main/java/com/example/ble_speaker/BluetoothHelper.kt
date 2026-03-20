package com.example.ble_speaker

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log

@SuppressLint("MissingPermission")
class BluetoothHelper(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var hearingAidProfile: BluetoothProfile? = null
    private var a2dpProfile: BluetoothProfile? = null

    var onDeviceFound: ((BluetoothDevice) -> Unit)? = null

    // ✅ profileListener defined BEFORE init so it exists when init runs
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.HEARING_AID -> {
                    hearingAidProfile = proxy
                    Log.d("BluetoothHelper", "ASHA Profile Connected")
                }
                BluetoothProfile.A2DP -> {
                    a2dpProfile = proxy
                    Log.d("BluetoothHelper", "A2DP Profile Connected")
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEARING_AID) hearingAidProfile = null
            if (profile == BluetoothProfile.A2DP) a2dpProfile = null
        }
    }

    // ✅ init runs AFTER profileListener is ready
    init {
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEARING_AID)
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                onDeviceFound?.invoke(device)
            }
        }
    }

    fun startScan() {
        Log.d("BluetoothHelper", "Starting BLE scan...")
        bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        Log.d("BluetoothHelper", "Stopping BLE scan...")
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun pairDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            Log.d("BluetoothHelper", "Pairing with ${device.name ?: device.address}")
            device.createBond()
        }
    }

    fun connectAudioProfile(device: BluetoothDevice) {
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            Log.d("BluetoothHelper", "Device not paired yet. Please pair first.")
            return
        }

        // Try ASHA first (for hearing aids)
        try {
            if (hearingAidProfile != null) {
                Log.d("BluetoothHelper", "Attempting ASHA connection...")
                val connectMethod = hearingAidProfile!!.javaClass
                    .getDeclaredMethod("connect", BluetoothDevice::class.java)
                connectMethod.isAccessible = true
                val success = connectMethod.invoke(hearingAidProfile, device) as? Boolean ?: false
                if (success) {
                    Log.d("BluetoothHelper", "ASHA connection triggered successfully.")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothHelper", "ASHA reflection failed: ${e.message}")
        }

        // Fallback to A2DP (for regular headphones/speakers)
        Log.d("BluetoothHelper", "Falling back to A2DP...")
        try {
            if (a2dpProfile != null) {
                val connectMethod = a2dpProfile!!.javaClass
                    .getDeclaredMethod("connect", BluetoothDevice::class.java)
                connectMethod.isAccessible = true
                connectMethod.invoke(a2dpProfile, device)
                Log.d("BluetoothHelper", "A2DP connection triggered successfully.")
            } else {
                Log.e("BluetoothHelper", "A2DP profile proxy not available.")
            }
        } catch (e: Exception) {
            Log.e("BluetoothHelper", "A2DP reflection failed: ${e.message}")
        }
    }

    fun cleanup() {
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEARING_AID, hearingAidProfile)
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile)
    }
}