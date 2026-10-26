# Bedah NusaSetup.exe (versi Windows)

## Bungkusnya
- `NusaSetup.exe` adalah installer **Inno Setup 6.7** (2,8 MB). Isinya satu berkas terkompres **LZMA2**: `Nusa.exe` (x64, 909 KB).
- `Nusa.exe` ditulis dengan **Rust** (crate `windows` 0.58). Gambar lewat **Direct2D + DirectWrite + DirectComposition + D3D11**, jadi notch digambar sendiri, bukan WebView.
- Hanya ada ikon (15 ukuran PNG, 16 sampai 256 px). Semua tampilan notch digambar lewat kode, jadi angka ukuran dan timing animasinya tidak tersimpan sebagai berkas; acuan yang dipakai adalah CSS di `nusa-main/index.html`, yang sama dengan yang dipakai versi Android.

## Fitur yang terbaca dari binary
| Fitur | Bukti di binary | Android |
|---|---|---|
| Notch tertutup: ring baterai, jam, equalizer | state "Closed" (idle) | Ada |
| Maskot mata, tidur, melambai | opsi tray "Nusa Mascot", `pet` | Ada (diperbaiki di 1.2.0) |
| Mode "Minimal notch" | tray menu, `minimal=` | Setara dengan mematikan maskot |
| Menu tray: Start with Windows, Nusa Mascot, Minimal notch, About, Exit | string UTF-16 | Tidak berlaku di Android |
| Musik + kontrol (SMTC) | `GlobalSystemMediaTransportControlsSessionManager` | Ada (MediaSession) |
| Lirik tersinkron | "Lyrics provided by LRCLIB", "Loading lyrics", "Lyrics not available" | Ada |
| Pomodoro: Focus Time / Short Break / Long Break | "Back to work, 25 min", "5 minute short break", "15 min, you earned it" | Fokus + istirahat; **istirahat panjang belum ada** |
| Pengingat air: botol, target 1,5 / 2,0 / 2,5 / 3,0 L, Snooze / Drank / Refilled / Reset | "Time to Drink", "Time to Refill", `water.txt` | Ada (tanpa Snooze dan "Refilled") |
| To-do: Add / Edit / Done Today / Later / Start | `todo.txt` | Ada (tanpa Edit) |
| Charger: Charging / Connected / Unplugged | string baterai | Ada |
| Baterai: Low / Critical / Fully charged | "Plug in soon", "Plug in now", "You can unplug" | Ada; **Penuh dan Kritis ditambah di 1.2.0** |
| Bluetooth tersambung | `Windows.Devices.Bluetooth`, "Connected" | Ada (headphone) |
| Status jaringan: Connected / No internet access / Disconnected | `NetworkInformation` | Belum |
| Caps Lock On/Off | "Caps Lock" | Tidak ada padanannya di HP |
| File shelf (seret file, "Drag out anytime") | `DoDragDrop`, folder `NusaShelf` | Tidak praktis di Android |
| Notifikasi aplikasi | `UserNotificationListener` | Belum |
| Sinkron suara notifikasi | `PlaySoundW` | Belum |
