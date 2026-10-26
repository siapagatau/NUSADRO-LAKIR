# Nusa untuk Android

Versi Android dari Nusa (notch hidup di atas layar), Kotlin + Jetpack Compose.
Tampilan notch mengikuti situs Nusa: notch hitam dengan telinga melengkung, ukuran dalam dp
(tertutup 150x30, panel 420x140, lirik 520x140, peek 310x60), maskot bermata, dan glow gradasi.
Animasi memakai timing yang sama dengan CSS situs (spring `cubic-bezier(.34,1.32,.5,1)` .62 s untuk lebar/tinggi,
radius dan telinga .45 s, lapisan keluar .2 s lalu masuk .32 s setelah jeda .2 s, kedip mata 4,2 s, equalizer .9 s,
lirik bergulir .7 s, bar air tumbuh bergiliran, busur peek 1 s, notch jatuh dari atas saat muncul).
Ini proyek independen, bukan port dari kode Windows-nya. Ikon aplikasi memakai `nusa-icon.png`.

## Baru di 1.3.4
- **Notch tidak lagi "melompat" saat selesai berpindah atau saat panel ditutup.** Penyebabnya: jendela overlay diubah ukuran/posisinya lewat WindowManager, tetapi gambarnya baru menyusul 1-2 frame kemudian, jadi notch sempat tampil di tempat lain lalu loncat ke posisi akhir. Perbaikan tampilan: teks daftar tugas kini tepat di tengah baris, bulatan placeholder thumbnail musik di tengah kotak,. Lirik satu baris di notch tertutup: bila terlalu panjang, teks dibungkus ke beberapa baris (maks 4) dan notch bertambah tinggi, tidak lagi dipotong "...". Mode lirik: offset geser baris dipasang sinkron saat baris berganti (tidak ada kedipan), dan isi panel lirik mengikuti tinggi panel yang beranimasi. Sekarang ada dua jendela (posisi searah tepi dianimasikan sebagai state pose, sinkron dengan frame Compose): jendela *visual* layar penuh yang tidak pernah berubah ukuran/posisi (semua gerak digambar Compose, termasuk geseran searah tepi yang dulu memakai animasi jendela), dan jendela *sentuh* kecil transparan yang meneruskan sentuhan ke jendela visual.

## Di 1.3.3
- **Guncang HP lebih peka**: ambang diturunkan (selisih dari gravitasi > 6 m/s2, 3 ayunan dalam ~1 detik), sensor dibaca lebih rapat, dan ada getar singkat saat guncangan terdeteksi sebagai tanda.

## Di 1.3.2
- **Lirik**: baris aktif kini selalu tepat di tengah kolom (posisi dihitung dari tinggi tiap baris, bukan dari scroll LazyColumn yang sebelumnya bisa berhenti di bawah), dan seluruh lirik meluncur halus saat baris berganti.
- **Ukuran notch mengikuti lirik**: tinggi panel lirik menyesuaikan jumlah baris lirik aktif; di layar sempit lirik memakai tata letak ringkas selebar layar (tanpa dikecilkan), jadi tidak terpotong dan tidak keluar layar.
- **Lirik saat notch ditutup**: bila lirik sedang ditampilkan lalu notch ditutup, baris aktif tetap tampil satu baris dan lebar notch menyesuaikan panjang barisnya. Ketuk untuk kembali ke panel lirik; matikan lewat ikon lirik.

## Di 1.3.1
- **Geser seperti karet/slime**: notch menahan titik yang kamu pegang (tidak lagi melompat ke jari), bisa ditarik menjauhi tepi sampai melar, lalu memantul saat dilepas. Sudut layar kini lebih lebar dan lentur (sudut bentuk menyusul dengan pegas), dan notch tidak lagi berputar mendadak saat jari melintas di tengah layar (pilihan tepi punya histeresis).
- **Gerak lebih halus**: saat digeser jendela overlay menjadi layar penuh dan diam, semua gerak digambar Compose (tidak ada pembaruan jendela tiap frame, sumber patah-patah dan "ngebling" sebelumnya). Peralihan jendela ke ukuran kecil dilakukan satu frame setelah pose final dipasang.
- **Ekspresi maskot**: dipegang/digeser -> kaget, ditarik kuat -> memicing, dilepas -> pusing 2 detik, guncang HP -> acak. Saat diam, maskot bermain sendiri: memantul (squash & stretch) atau berjoged kanan-kiri sambil tersenyum.
- **Tugas langsung di notch**: tambah (Tambah), ubah (ketuk judul), centang (lingkaran), hapus (x), dan "Bersih" untuk menghapus yang selesai. Kolom ketik membuat jendela notch bisa fokus hanya selama mengetik (keyboard muncul). Tombol lonceng = ingatkan 1 jam lagi.
- **Aplikasi**: label status baru (bertinta hijau dengan halo) dan navigasi bawah melayang berbentuk kapsul dengan item aktif bergradasi yang melebar.

## Di 1.3.0
- **Notch bisa digeser ke mana saja**: tahan lalu geser di sekeliling layar. Pengaturan posisi di aplikasi dihapus (diganti tombol "Kembalikan ke tengah atas"). Di sudut notch membelok seperti slime (meregang saat cepat, memantul saat dilepas), lalu menempel di tepi. Bentuknya berputar mengikuti tepi; di tepi bawah bentuknya terbalik tetapi tulisan tetap tegak; di kiri/kanan seluruhnya diputar 90 derajat.
- **Glitch saat notch menutup diperbaiki**: penempatan notch kini murni dari ukuran jendela (menempel di tepi, terpusat searah tepi), tanpa koordinat layar yang harus sinkron, jadi jendela boleh berubah ukuran tanpa notch melompat ke tengah lalu kembali.
- **Animasi lebih ringan**: skala muat-layar lewat graphicsLayer (bukan mengubah kepadatan, yang dulu membuat semua teks diukur ulang tiap frame), nilai animasi dibaca di layout/gambar saja (tanpa rekomposisi per frame), dan APK kini build rilis (R8 + shrink) yang jauh lebih cepat daripada debug.
- **Guncang HP**: maskot tiba-tiba muncul dengan ekspresi acak (kaget, pusing, senang, memicing). Bisa dimatikan di Atur.
- **Tombol air**: kini jelas, "+250 ml" (sesuai ukuran botol). Teks tombol lain tidak lagi terpotong.

## Di 1.2.0
- **Posisi notch disederhanakan**: fitur geser bebas (horizontal, jarak atas, ukuran) dihapus karena notch jadi berkedip. Sekarang saat HP berdiri notch menempel di **sisi kiri atau kanan** layar (pilih di tab Atur), dan saat landscape otomatis di tengah atas. Jendela overlay tidak lagi di-update bila ukurannya tidak berubah.
- **Maskot selalu kembali**: begitu notch sepi (tanpa musik, timer, atau pemberitahuan) mata muncul lagi, tanpa menunggu 8 detik dan tidak lagi "sekali saja". Matanya berkedip acak, melirik sesekali, tidur setelah 20 detik tanpa disentuh, lalu melambai saat kamu kembali. Maskot di header aplikasi dihapus.
- **Tombol izin tidak lagi terasa mati**: sebabnya animasi tak berujung (kedip, napas, equalizer) yang membuat overlay dan aplikasi merender 60 fps terus. Sekarang kedip/napas digerakkan hanya saat bergerak, equalizer hanya hidup saat musik main, dan pelacak sentuhan di seluruh aplikasi dibuang. Semua tombol memberi umpan balik instan (mengecil saat disentuh). Bila dialog izin notifikasi sudah tidak bisa muncul, Nusa langsung membuka pengaturan notifikasinya.
- **UI/UX aplikasi dirombak**: Beranda jadi dashboard (status, pratinjau, tombol nyalakan, checklist izin dengan progres, ringkasan tugas/air/timer), navigasi bawah baru dengan indikator animasi dan transisi antar tab, tombol Kembali kembali ke Beranda dulu, dan tab terakhir diingat saat layar diputar.
- Baru dari versi Windows: notifikasi **baterai penuh** ("Boleh dicabut") dan **baterai kritis**.

## Fitur
- Notch overlay di atas layar: tertutup (cincin baterai, jam, equalizer), maskot bermata saat sepi, peek untuk charger, headphone, dan air minum
- Maskot: begitu notch sepi mata muncul (berkedip, melirik); 20 detik tanpa disentuh maskot tidur (kelopak turun, napas pelan, zZz). Sentuhan di layar, layar menyala, atau buka kunci membangunkannya dan ia melambai (mata membuka dengan pantulan, lalu bergoyang)
- Ketuk untuk membuka; geser kiri/kanan atau ketuk panah untuk pindah halaman (Musik, Pomodoro, Air, To-do); ketuk di luar untuk menutup
- Musik dari pemutar mana pun (Spotify, YouTube, dll.) + kontrol putar, jeda, lanjut, seek
- Lirik tersinkron dari [LRCLIB](https://lrclib.net), ketuk baris untuk lompat
- Timer Pomodoro, pengingat minum air, to-do dengan pengingat
- Info charger, baterai lemah, dan headphone terhubung

## Build APK di GitHub
1. Buat repo baru di GitHub, lalu push isi folder ini ke branch `main`.
2. Buka tab **Actions** -> workflow **Build Android APK** jalan otomatis (atau klik **Run workflow**).
3. Setelah hijau, buka run-nya -> **Artifacts** -> unduh **Nusa-apk** (isinya `Nusa.apk`).
4. Untuk membuat halaman Release dengan APK terlampir:
   `git tag v1.0.0 && git push origin v1.0.0`

## Pasang di HP
1. Pasang APK, buka Nusa.
2. Beri 3 izin di layar Beranda (tampil di atas aplikasi lain, akses notifikasi, kirim notifikasi).
3. Ketuk **Aktifkan notch**.

Catatan Android 13+: untuk APK yang dipasang manual, "Akses notifikasi" bisa terkunci.
Buka Info aplikasi Nusa -> menu ⋮ -> **Izinkan setelan terbatas**, lalu coba lagi.

## Build lokal (opsional)
Butuh JDK 17 dan Android SDK 34: `gradle assembleDebug`
(atau jalankan `gradle wrapper` sekali untuk membuat `gradlew`).

## Batasan
- APK yang dihasilkan bertanda tangan debug (cukup untuk dipasang sendiri, belum untuk Play Store).
- Menambah tugas dan mengubah pengaturan dilakukan di aplikasi, bukan di notch (overlay tidak bisa mengetik).
- Pengingat air minum berjalan selama notch aktif; pengingat tugas memakai alarm sistem (bisa telat beberapa menit saat mode hemat daya).
- Tidak ada file shelf (drag file) karena tidak praktis di Android.
