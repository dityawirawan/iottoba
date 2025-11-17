<img width="540" height="1170" alt="iottoba" src="https://github.com/user-attachments/assets/f794dffe-cc4f-44ff-a6f8-a996be619c5f" />

# IoTToba
Aplikasi Android berbasis Kotlin untuk mengontrol dan memonitor perangkat IoT (otomasi pakan ikan) melalui Firebase Realtime Database.  
IoTToba menyediakan kontrol manual (ON/OFF) serta pengaturan jadwal otomatis menggunakan time picker.

---

## 📱 Fitur Utama

### 🔌 1. Kontrol Manual Relay
- Menghidupkan atau mematikan *relay1* secara langsung.
- Status relay disinkronkan dengan Firebase secara realtime.
- UI otomatis menyesuaikan perubahan status dari server.

### ⏰ 2. Pengaturan Jadwal Otomatis
- Terdapat dua jadwal:
  - **Jadwal 1**
  - **Jadwal 2**
- Masing-masing bisa diatur jamnya melalui *TimePickerDialog*.
- Jadwal otomatis disimpan ke Firebase.
- UI menampilkan jadwal terbaru dari database.

### 📡 3. Monitoring Data Realtime
- Mendengarkan perubahan node:
  - `/sensor` → data sensor (jika digunakan)
  - `/perintah` → status relay + jadwal
- Update UI otomatis ketika data berubah.

### 🌙 4. Mode Gelap (Dark Mode)
- Menggunakan `AppCompatDelegate` untuk menetapkan Night Mode secara default.

### 📲 5. UI Modern & Responsif
- SwitchCompat untuk toggle.
- ImageButton untuk kontrol cepat.
- ProgressBar untuk loading states.
- Toast & Log debugging untuk komunikasi status.

---

## 🏗️ Arsitektur Firebase

Struktur database yang digunakan:

```

/
├── sensor
│   └── ... (opsional, untuk monitoring)
└── perintah
├── relay1: true/false
├── jadwal1: "HH:mm"
└── jadwal2: "HH:mm"

```

Aplikasi membaca & menulis 3 node utama:
- `relay1` → kontrol manual
- `jadwal1` → jam otomatis 1
- `jadwal2` → jam otomatis 2

Semua perubahan terjadi secara realtime.

---

## 📂 Struktur Project

Struktur utama:

```

IoTToba/
│── app/
│   ├── src/main/
│   │   ├── java/com/example/iottoba/
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   ├── google-services.json
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── gradle/
│── settings.gradle.kts
└── build.gradle.kts

````

---

## 🧠 Penjelasan MainActivity (Singkat)
MainActivity menangani:

### 🔹 Inisialisasi Firebase
```kotlin
db = FirebaseDatabase.getInstance()
rootRef = db.reference
perintahRef = rootRef.child("perintah")
sensorRef = rootRef.child("sensor")
````

### 🔹 Listener Realtime

* Mendengar perubahan `/perintah`
* Update switch, jadwal, dan UI otomatis

### 🔹 Kontrol Manual Relay

```kotlin
perintahRef.child("relay1").setValue(value)
```

### 🔹 Pengaturan Waktu

Menggunakan `TimePickerDialog` untuk memilih jam.

### 🔹 Error Handling

* Try/catch di setiap operasi Firebase
* Logging detail (`Log.e`, `Log.d`)
* Toast feedback untuk user

---

## 🧑‍💻 Cara Menjalankan Project

### 1. Clone Repo

```bash
git clone https://github.com/dityawirawan/iottoba.git
cd iottoba
```

### 2. Buka di Android Studio

* Pastikan menggunakan **Android Studio Flamingo / Hedgehog / terbaru**
* Pastikan JDK 17+

### 3. Tambahkan Firebase Config

Pastikan file:

```
app/google-services.json
```

sudah terisi.
Jika belum, tambahkan melalui Firebase Console.

### 4. Jalankan aplikasi

* Via emulator
* Atau perangkat fisik (USB debugging aktif)

---

## 🔥 Build APK

```bash
./gradlew assembleRelease
```

File APK akan muncul di:

```
app/build/outputs/apk/
```

---

## 🤝 Kontribusi

Silakan buat Issue atau Pull Request untuk perbaikan & fitur baru.

---

## 📄 Lisensi

Lisensi belum ditentukan. Tambahkan MIT/Apache GPL sesuai kebutuhan.



