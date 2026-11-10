# Kayzen Auto Buy Android v2.6

## Fitur
- Dashboard gelap modern dengan status Accessibility dan kontrol pemantauan.
- Tombol untuk membuka aplikasi DANA.
- Heads-up notification prioritas tinggi dengan getaran saat target voucher terkonfirmasi.
- Floating pop-up di atas tampilan DANA menggunakan jenis jendela Accessibility overlay; tidak meminta izin tampil di atas aplikasi lain.
- Pop-up menampilkan jumlah Diamond, dapat ditutup manual, dan menutup otomatis setelah 9 detik.
- Pemindaian cepat berbasis event Accessibility dan pemeriksaan berkala.
- Pencocokan target 7290, 355, dan 100 Diamond dengan konfirmasi tiga pemindaian.
- Pemantauan berhenti setelah kartu berhasil dibuka.

## Batasan keamanan
- Hanya mencoba membuka kartu voucher dengan teks aksi eksplisit Beli/Klaim/Ambil/Redeem.
- Tidak melakukan checkout, pembayaran, PIN, OTP, CAPTCHA, atau melewati antrean.
- Tidak membaca/mengirim saldo atau kredensial.
- Hanya memproses tampilan DANA (id.dana).
- Jika UI ambigu atau area klik tidak aman, tidak mengeklik dan meminta tindakan manual.
- Heads-up notification dapat dipengaruhi izin notifikasi dan pengaturan channel Android/Xiaomi.
- Floating overlay bergantung pada layanan Accessibility Kayzen tetap aktif.
- Deteksi UI DANA harus diuji langsung; kompatibilitas tidak dijamin.

## Build
GitHub Actions membangun APK debug pada push ke main. Artifact: kayzen-auto-buy-debug-apk.
Membutuhkan Android SDK 35 dan JDK 17.
