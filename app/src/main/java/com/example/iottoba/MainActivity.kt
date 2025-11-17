package com.example.iottoba

import android.app.TimePickerDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*
import java.util.Locale
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var batteryProgressBar: ProgressBar
    private lateinit var tvBatteryPercentage: TextView
    private lateinit var progressPakan1: ProgressBar
    private lateinit var progressPakan2: ProgressBar
    private lateinit var tvPakan1: TextView
    private lateinit var tvPakan2: TextView

    // Jadwal 1
    private lateinit var tvJadwal1: TextView
    private lateinit var tvJadwal1Alat2: TextView
    private lateinit var btnCalendar1: ImageButton
    private lateinit var btnCalendar1Alat2: ImageButton

    // Jadwal 2
    private lateinit var tvJadwal2: TextView
    private lateinit var tvJadwal2Alat2: TextView
    private lateinit var btnCalendar2: ImageButton
    private lateinit var btnCalendar2Alat2: ImageButton

    private lateinit var switchManual1: SwitchCompat
    private lateinit var switchManual2: SwitchCompat

    // Firebase
    private lateinit var db: FirebaseDatabase
    private lateinit var rootRef: DatabaseReference
    private lateinit var sensorRef: DatabaseReference
    private lateinit var perintahRef: DatabaseReference

    // Hindari loop saat kita set nilai akibat pembaruan dari DB
    private var updatingFromDb = false

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Lock orientasi ke portrait
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            // Force light theme (tema terang)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            setContentView(R.layout.activity_main)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            initViews()
            initFirebase()
            observeSensor()
            observePerintah()
            setupListeners()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        try {
            batteryProgressBar = findViewById(R.id.batteryProgressBar)
            tvBatteryPercentage = findViewById(R.id.tvBatteryPercentage)

            progressPakan1 = findViewById(R.id.progressPakan1)
            progressPakan2 = findViewById(R.id.progressPakan2)
            tvPakan1 = findViewById(R.id.tvPakan1)
            tvPakan2 = findViewById(R.id.tvPakan2)

            // Jadwal 1
            tvJadwal1 = findViewById(R.id.tvJadwal1)
            tvJadwal1Alat2 = findViewById(R.id.tvJadwal1Alat2)
            btnCalendar1 = findViewById(R.id.btnCalendar1)
            btnCalendar1Alat2 = findViewById(R.id.btnCalendar1Alat2)

            // Jadwal 2
            tvJadwal2 = findViewById(R.id.tvJadwal2)
            tvJadwal2Alat2 = findViewById(R.id.tvJadwal2Alat2)
            btnCalendar2 = findViewById(R.id.btnCalendar2)
            btnCalendar2Alat2 = findViewById(R.id.btnCalendar2Alat2)

            switchManual1 = findViewById(R.id.switchManual1)
            switchManual2 = findViewById(R.id.switchManual2)

            // Set default values untuk pakan2 (tidak aktif tapi terlihat)
            progressPakan2.progress = 0
            tvPakan2.text = "0%"

            // Disable pakan2 controls
            switchManual2.isEnabled = false
            btnCalendar1Alat2.isEnabled = false
            btnCalendar2Alat2.isEnabled = false

            Log.d(TAG, "All views initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }

    private fun initFirebase() {
        try {
            // Pakai URL instance sesuai region milikmu
            db = FirebaseDatabase.getInstance(
                "https://iottoba-default-rtdb.asia-southeast1.firebasedatabase.app"
            )
            rootRef = db.reference
            sensorRef = rootRef.child("data_sensor")
            perintahRef = rootRef.child("perintah")

            Log.d(TAG, "Firebase initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            Toast.makeText(this, "Error connecting to database", Toast.LENGTH_SHORT).show()
        }
    }

    // ===================== OBSERVE (UI <- DB) =====================

    private fun observeSensor() {
        try {
            sensorRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // sisaPakan -> progress pakan 1
                        val sisaPakan = snapshot.child("sisaPakan").getValue(Int::class.java) ?: 0
                        updatePakan1(clamp(sisaPakan, 0, 100))

                        // Pakan2 tetap 0% (tidak aktif)
                        updatePakan2(0)

                        // voltage -> estimasi persen baterai (model sederhana 10.5V=0%, 12.6V=100%)
                        val voltAny = snapshot.child("voltage").value
                        val volt = when (voltAny) {
                            is Long -> voltAny.toDouble()
                            is Int -> voltAny.toDouble()
                            is Double -> voltAny
                            is String -> voltAny.toDoubleOrNull()
                            else -> null
                        }
                        val battPct = if (volt != null) {
                            val pct = (volt - 11.8) / (13.6 - 11.8) * 100.0
                            clamp(pct.toInt(), 0, 100)
                        } else 0

                        updateBattery(battPct)

                        Log.d(TAG, "Sensor data updated - Pakan: $sisaPakan%, Battery: $battPct%, Voltage: $volt")

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing sensor data: ${e.message}", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Sensor observation cancelled: ${error.message}")
                    Toast.makeText(this@MainActivity, "Error reading sensor data", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sensor observer: ${e.message}", e)
        }
    }

    private fun observePerintah() {
        try {
            perintahRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val relay1 = snapshot.child("relay1").getValue(Boolean::class.java) ?: false

                        // Tandai bahwa update berasal dari DB agar listener tidak menulis balik
                        updatingFromDb = true
                        switchManual1.isChecked = relay1
                        switchManual1.text = if (relay1) "ON" else "OFF"
                        updatingFromDb = false

                        // Jadwal 1 pakan alat 1
                        val jam1 = snapshot.child("makan1_jam").getValue(Int::class.java)
                        val mnt1 = snapshot.child("makan1_mnt").getValue(Int::class.java)
                        if (jam1 != null && mnt1 != null) {
                            tvJadwal1.text = String.format(Locale.getDefault(), "%02d:%02d", jam1, mnt1)
                        } else {
                            tvJadwal1.text = "--:--"
                        }

                        // Jadwal 2 pakan alat 1
                        val jam2 = snapshot.child("makan2_jam").getValue(Int::class.java)
                        val mnt2 = snapshot.child("makan2_mnt").getValue(Int::class.java)
                        if (jam2 != null && mnt2 != null) {
                            tvJadwal2.text = String.format(Locale.getDefault(), "%02d:%02d", jam2, mnt2)
                        } else {
                            tvJadwal2.text = "--:--"
                        }

                        // Alat 2 belum ada key - tampilkan placeholder
                        tvJadwal1Alat2.text = "--:--"
                        tvJadwal2Alat2.text = "--:--"

                        Log.d(TAG, "Perintah data updated - Relay1: $relay1, Jadwal1: ${tvJadwal1.text}, Jadwal2: ${tvJadwal2.text}")

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing perintah data: ${e.message}", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Perintah observation cancelled: ${error.message}")
                    Toast.makeText(this@MainActivity, "Error reading command data", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up perintah observer: ${e.message}", e)
        }
    }

    // ===================== ACTION (UI -> DB) =====================

    private fun setupListeners() {
        try {
            // Calendar button untuk jadwal 1 alat 1
            btnCalendar1.setOnClickListener {
                showTimePickerDialog(scheduleType = 1, device = 1)
            }

            // Calendar button untuk jadwal 2 alat 1
            btnCalendar2.setOnClickListener {
                showTimePickerDialog(scheduleType = 2, device = 1)
            }

            // Calendar button untuk jadwal 1 alat 2 (disabled)
            btnCalendar1Alat2.setOnClickListener {
                Toast.makeText(this, "Alat 2 belum tersedia", Toast.LENGTH_SHORT).show()
            }

            // Calendar button untuk jadwal 2 alat 2 (disabled)
            btnCalendar2Alat2.setOnClickListener {
                Toast.makeText(this, "Alat 2 belum tersedia", Toast.LENGTH_SHORT).show()
            }

            // Pakan manual 1: toggle menulis true/false ke Firebase
            switchManual1.setOnCheckedChangeListener { _, isChecked ->
                try {
                    // Jika perubahan berasal dari pembaruan DB, abaikan (hindari loop)
                    if (updatingFromDb) return@setOnCheckedChangeListener

                    // Update text segera di UI
                    switchManual1.text = if (isChecked) "ON" else "OFF"

                    // Tulis ke Firebase nilai yang dipilih (true/false)
                    sendRelayCommand(isChecked)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in switchManual1 listener: ${e.message}", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Pakan manual 2: disabled (belum tersedia)
            switchManual2.setOnCheckedChangeListener { _, _ ->
                if (!updatingFromDb) {
                    Toast.makeText(this, "Alat 2 belum tersedia", Toast.LENGTH_SHORT).show()
                    updatingFromDb = true
                    switchManual2.isChecked = false
                    updatingFromDb = false
                }
            }

            Log.d(TAG, "Listeners setup successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listeners: ${e.message}", e)
        }
    }

    private fun showTimePickerDialog(scheduleType: Int, device: Int) {
        try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                try {
                    when (device) {
                        1 -> {
                            // Tentukan path Firebase berdasarkan scheduleType
                            val jamPath = if (scheduleType == 1) "makan1_jam" else "makan2_jam"
                            val mntPath = if (scheduleType == 1) "makan1_mnt" else "makan2_mnt"
                            val scheduleLabel = if (scheduleType == 1) "Jadwal 1" else "Jadwal 2"

                            // Tulis jam terlebih dahulu
                            perintahRef.child(jamPath).setValue(selectedHour)
                                .addOnSuccessListener {
                                    Log.d(TAG, "$scheduleLabel hour saved: $selectedHour")

                                    // Setelah 5 detik, kirim menit
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        try {
                                            perintahRef.child(mntPath).setValue(selectedMinute)
                                                .addOnSuccessListener {
                                                    Log.d(TAG, "$scheduleLabel minute saved: $selectedMinute")
                                                    Toast.makeText(
                                                        this,
                                                        "$scheduleLabel disimpan: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(TAG, "Failed to save $scheduleLabel minute: ${e.message}", e)
                                                    Toast.makeText(this, "Gagal menyimpan menit $scheduleLabel", Toast.LENGTH_SHORT).show()
                                                }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error while delayed-saving minute: ${e.message}", e)
                                            Toast.makeText(this, "Error menyimpan menit $scheduleLabel", Toast.LENGTH_SHORT).show()
                                        }
                                    }, 5000L)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to save $scheduleLabel hour: ${e.message}", e)
                                    Toast.makeText(this, "Gagal menyimpan jam $scheduleLabel", Toast.LENGTH_SHORT).show()
                                }
                        }
                        2 -> {
                            // Alat 2 belum tersedia
                            Toast.makeText(this, "Alat 2 belum tersedia", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving schedule: ${e.message}", e)
                    Toast.makeText(this, "Gagal menyimpan jadwal", Toast.LENGTH_SHORT).show()
                }
            }, hour, minute, true).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing time picker: ${e.message}", e)
            Toast.makeText(this, "Error menampilkan pemilih waktu", Toast.LENGTH_SHORT).show()
        }
    }

    // ===================== HELPERS =====================

    /**
     * Mengirim nilai relay1 ke Firebase. Jika gagal, UI akan di-rollback.
     */
    private fun sendRelayCommand(value: Boolean) {
        try {
            perintahRef.child("relay1").setValue(value)
                .addOnSuccessListener {
                    Log.d(TAG, "Sent relay1 = $value to Firebase")
                    Toast.makeText(this, "Perintah relay disimpan: ${if (value) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send relay1 command: ${e.message}", e)
                    Toast.makeText(this, "Gagal mengirim perintah", Toast.LENGTH_SHORT).show()
                    // Rollback UI jika gagal menulis
                    updatingFromDb = true
                    switchManual1.isChecked = !value
                    switchManual1.text = if (!value) "ON" else "OFF"
                    updatingFromDb = false
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendRelayCommand: ${e.message}", e)
        }
    }

    // ===================== UTIL =====================

    private fun updateBattery(percentage: Int) {
        try {
            batteryProgressBar.progress = percentage
            tvBatteryPercentage.text = "$percentage%"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating battery: ${e.message}", e)
        }
    }

    private fun updatePakan1(percentage: Int) {
        try {
            progressPakan1.progress = percentage
            tvPakan1.text = "$percentage%"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pakan1: ${e.message}", e)
        }
    }

    private fun updatePakan2(percentage: Int) {
        try {
            progressPakan2.progress = percentage
            tvPakan2.text = "$percentage%"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pakan2: ${e.message}", e)
        }
    }

    private fun clamp(v: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, v))

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Cleanup Firebase listeners jika perlu
            Log.d(TAG, "Activity destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }
}