# Kayzen Auto Buy Android v2.4

## Fitur
- Dashboard status Accessibility Service (aktif/belum aktif).
- Tombol jeda dan lanjutkan pemantauan.
- Tombol BUKA DANA untuk membuka aplikasi DANA langsung dari Kayzen.
- Log aktivitas terakhir.
- Pemindaian berkala setiap 1,5 detik ketika pemantauan aktif.
- Pencocokan target 7290, 355, dan 100 Diamond dengan konfirmasi tiga pemindaian.
- Pemantauan berhenti setelah kartu berhasil dibuka.

## Batasan keamanan
- Hanya mencoba membuka kartu voucher dengan teks aksi eksplisit Beli/Klaim/Ambil/Redeem.
- Tidak melakukan checkout, pembayaran, PIN, OTP, CAPTCHA, atau melewati antrean.
- Tidak membaca/mengirim saldo atau kredensial.
- Hanya memproses tampilan DANA (id.dana).
- Jika UI ambigu atau area klik tidak aman, tidak mengeklik dan meminta tindakan manual.
- Deteksi UI DANA harus diuji langsung; kompatibilitas tidak dijamin.

## Build
GitHub Actions membangun APK debug pada push ke main. Artifact: kayzen-auto-buy-debug-apk.
Membutuhkan Android SDK 35 dan JDK 17.
