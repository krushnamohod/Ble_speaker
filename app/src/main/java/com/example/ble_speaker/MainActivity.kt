package com.example.ble_speaker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // ── Color Palette ──
    private val colorBgDark       = Color.parseColor("#0D1117")
    private val colorSurface      = Color.parseColor("#161B22")
    private val colorAccent       = Color.parseColor("#58A6FF")
    private val colorAccentDim    = Color.parseColor("#1F6FEB")
    private val colorTextPrimary  = Color.parseColor("#E6EDF3")
    private val colorTextSecondary= Color.parseColor("#8B949E")
    private val colorDivider      = Color.parseColor("#21262D")
    private val colorRed          = Color.parseColor("#F85149")
    private val colorGreen        = Color.parseColor("#3FB950")

    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var deviceListView: ListView
    private lateinit var connectButton: FrameLayout
    private lateinit var connectButtonText: TextView
    private lateinit var volumeSlider: SeekBar
    private lateinit var statusText: TextView
    private lateinit var audioManager: AudioManager
    private lateinit var volumeLabel: TextView

    private val deviceNames = mutableListOf<String>()
    private val devices = mutableListOf<android.bluetooth.BluetoothDevice>()
    private lateinit var arrayAdapter: ArrayAdapter<String>

    private var selectedDevice: android.bluetooth.BluetoothDevice? = null

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            setupBluetooth()
        } else {
            Toast.makeText(this, "Permissions required for BLE Speaker", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = colorBgDark
        window.navigationBarColor = colorBgDark

        // ── Root Layout ──
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBgDark)
            setPadding(dp(20), dp(48), dp(20), dp(24))
        }

        // ── App Logo ──
        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(8)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // ── Title ──
        val titleText = TextView(this).apply {
            text = "BLE Speaker"
            textSize = 28f
            setTextColor(colorTextPrimary)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }

        // ── Subtitle ──
        val subtitleText = TextView(this).apply {
            text = "Bluetooth Audio Streaming"
            textSize = 13f
            setTextColor(colorTextSecondary)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        }

        // ── Status Card ──
        statusText = TextView(this).apply {
            text = "● Disconnected"
            textSize = 14f
            setTextColor(colorRed)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(colorSurface)
                cornerRadius = dp(12).toFloat()
            }
        }

        // ── Spacer ──
        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16)
            )
        }

        // ── Devices Label ──
        val devicesLabel = TextView(this).apply {
            text = "NEARBY DEVICES"
            textSize = 11f
            setTextColor(colorTextSecondary)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.1f
            setPadding(dp(4), 0, 0, dp(8))
        }

        // ── Device List Card ──
        val listCard = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(colorSurface)
                cornerRadius = dp(16).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
            ).apply { bottomMargin = dp(20) }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        deviceListView = ListView(this).apply {
            divider = GradientDrawable().apply {
                setColor(colorDivider)
                setSize(0, 1)
            }
            dividerHeight = 1
            setSelector(android.R.color.transparent)
        }
        listCard.addView(deviceListView)

        // ── Bottom Controls: Circular Button + Vertical Volume ──
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // ── Circular Connect Button ──
        val buttonSize = dp(110)
        connectButton = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                marginEnd = dp(32)
            }
            isClickable = true
            isFocusable = true
            alpha = 0.4f // Disabled look
            background = createCircleDrawable()
            elevation = dp(8).toFloat()
        }

        connectButtonText = TextView(this).apply {
            text = "▶\nStream"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        connectButton.addView(connectButtonText)

        // ── Vertical Volume Control ──
        val volumeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Initialize AudioManager for system volume
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val volTopLabel = TextView(this).apply {
            text = "🔊"
            textSize = 16f
            gravity = Gravity.CENTER
        }

        // Vertical SeekBar via rotation
        val sliderContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(180))
        }

        volumeSlider = SeekBar(this).apply {
            max = maxVol
            progress = curVol
            rotation = 270f // Make it vertical
            layoutParams = FrameLayout.LayoutParams(dp(180), dp(48)).apply {
                gravity = Gravity.CENTER
            }
            thumb?.setTint(colorAccent)
            progressDrawable?.setTint(colorAccent)
        }
        sliderContainer.addView(volumeSlider)

        val volBottomLabel = TextView(this).apply {
            text = "🔇"
            textSize = 16f
            setTextColor(colorTextSecondary)
            gravity = Gravity.CENTER
        }

        volumeLabel = TextView(this).apply {
            text = "VOL"
            textSize = 10f
            setTextColor(colorTextSecondary)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.15f
            setPadding(0, dp(4), 0, 0)
        }

        volumeContainer.addView(volTopLabel)
        volumeContainer.addView(sliderContainer)
        volumeContainer.addView(volBottomLabel)
        volumeContainer.addView(volumeLabel)

        bottomRow.addView(connectButton)
        bottomRow.addView(volumeContainer)

        // ── Assemble Root ──
        root.addView(logoImage)
        root.addView(titleText)
        root.addView(subtitleText)
        root.addView(statusText)
        root.addView(spacer1)
        root.addView(devicesLabel)
        root.addView(listCard)
        root.addView(bottomRow)

        setContentView(root)

        // ── Custom Adapter for dark-themed list items ──
        arrayAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(colorTextPrimary)
                    textSize = 14f
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setBackgroundColor(Color.TRANSPARENT)
                }
                return view
            }
        }
        deviceListView.adapter = arrayAdapter

        checkPermissionsAndInit()
        setupUIListeners()
    }

    private fun createCircleDrawable(): StateListDrawable {
        val pressed = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorAccentDim)
        }
        val normal = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(colorAccent, colorAccentDim)
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(60).toFloat()
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun createStopCircleDrawable(): StateListDrawable {
        val pressed = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#B62324"))
        }
        val normal = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(colorRed, Color.parseColor("#B62324"))
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(60).toFloat()
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun checkPermissionsAndInit() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            setupBluetooth()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupBluetooth() {
        bluetoothHelper = BluetoothHelper(this)
        bluetoothHelper.onDeviceFound = { device ->
            runOnUiThread {
                val name = try { device.name ?: "Unknown Device" } catch(e: SecurityException) { "Unknown Device" }
                val address = device.address
                val entry = "$name\n$address"
                if (!deviceNames.contains(entry)) {
                    deviceNames.add(entry)
                    devices.add(device)
                    arrayAdapter.notifyDataSetChanged()
                }
            }
        }
        bluetoothHelper.startScan()
        statusText.text = "● Scanning for devices..."
        statusText.setTextColor(colorAccent)
    }

    @SuppressLint("MissingPermission")
    private fun setupUIListeners() {
        deviceListView.setOnItemClickListener { _, view, position, _ ->
            selectedDevice = devices[position]
            val safeName = try { selectedDevice?.name ?: "Unknown" } catch(e: SecurityException) { "Unknown" }
            Toast.makeText(this, "Selected: $safeName", Toast.LENGTH_SHORT).show()

            // Enable button visually
            connectButton.alpha = 1.0f
            connectButton.isEnabled = true

            // Highlight selected row
            for (i in 0 until deviceListView.childCount) {
                deviceListView.getChildAt(i)?.setBackgroundColor(Color.TRANSPARENT)
            }
            view.setBackgroundColor(Color.parseColor("#1C2333"))
        }

        connectButton.setOnClickListener {
            if (selectedDevice == null && !AudioStreamService.isStreaming) return@setOnClickListener

            if (AudioStreamService.isStreaming) {
                // Stop
                val stopIntent = Intent(this, AudioStreamService::class.java).apply {
                    action = AudioStreamService.ACTION_STOP
                }
                startService(stopIntent)
                connectButtonText.text = "▶\nStream"
                connectButton.background = createCircleDrawable()
                statusText.text = "● Disconnected"
                statusText.setTextColor(colorRed)
            } else {
                selectedDevice?.let { device ->
                    val safeName = try { device.name ?: device.address } catch(e: SecurityException) { "Device" }
                    statusText.text = "● Connecting to $safeName..."
                    statusText.setTextColor(Color.parseColor("#D29922"))

                    bluetoothHelper.pairDevice(device)
                    bluetoothHelper.connectAudioProfile(device)

                    val startIntent = Intent(this, AudioStreamService::class.java).apply {
                        action = AudioStreamService.ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(startIntent)
                    } else {
                        startService(startIntent)
                    }
                    connectButtonText.text = "■\nStop"
                    connectButton.background = createStopCircleDrawable()
                    statusText.text = "● Streaming Audio"
                    statusText.setTextColor(colorGreen)
                }
            }
        }

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothHelper.isInitialized) {
            bluetoothHelper.stopScan()
            bluetoothHelper.cleanup()
        }
    }
}
