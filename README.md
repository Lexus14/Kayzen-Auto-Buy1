# Kayzen Auto Buy Android v2.3

## Fitur
- Tombol Mulai/Hentikan Pemantauan.
- Dashboard status dan aktivitas terakhir.
- Pemindaian berkala setiap 1,5 detik ketika diaktifkan.
- Pencocokan nominal 7290, 355, dan 100 Diamond yang lebih ketat.
- Konfirmasi tiga pemindaian sebelum mencoba membuka kartu.
- Pemantauan berhenti setelah klik berhasil.

## Batasan keamanan
- Hanya membuka kartu voucher dengan teks aksi eksplisit Beli/Klaim/Ambil/Redeem.
- Tidak melakukan checkout, pembayaran, PIN, OTP, CAPTCHA, atau melewati antrean.
- Tidak membaca/mengirim saldo atau kredensial.
- Hanya memproses tampilan DANA (id.dana).
- Jika UI ambigu atau tidak cocok, aplikasi tidak mengeklik dan meminta tindakan manual.

## Build
GitHub Actions membangun APK debug pada push ke main. Artifact: kayzen-auto-buy-debug-apk.
Membutuhkan Android SDK 35 dan JDK 17. Deteksi harus diuji langsung; kompatibilitas UI DANA tidak dijamin.