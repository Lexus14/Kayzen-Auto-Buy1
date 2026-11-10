# Kayzen Auto Buy Android v2.2

Aplikasi pendamping Android berbasis Accessibility Service untuk membantu memantau kartu voucher DANA Deals.

## Batasan keamanan
- Hanya mencoba klik kartu voucher ketika ada teks aksi eksplisit: Beli/Klaim/Ambil/Redeem.
- Tidak menekan checkout, pembayaran, konfirmasi pembayaran, PIN, OTP, atau CAPTCHA.
- Tidak melewati antrean atau mekanisme keamanan.
- Tidak membaca atau mengirim saldo, kredensial, maupun data pembayaran.
- Hanya memproses tampilan aplikasi DANA (id.dana).

## Prioritas target
1. 7290 Diamond
2. 355 Diamond
3. 100 Diamond

Jika target prioritas lebih tinggi tidak terlihat atau statusnya tidak jelas, layanan tidak beralih ke target lebih rendah. Jika stok habis terdeteksi, target tersebut dilewati.

## Build APK
Workflow GitHub Actions berjalan otomatis saat commit ke branch main atau dapat dijalankan manual dari tab Actions. Hasil build diunggah sebagai artifact `kayzen-auto-buy-debug-apk` berisi `app-debug.apk`.

## Catatan
Membutuhkan Android SDK 35 dan JDK 17. Struktur accessibility DANA dapat berbeda menurut versi aplikasi, wilayah, atau eksperimen UI. Jika teks kartu tidak diekspos sebagai node aksesibilitas, layanan tidak akan mendeteksinya. Uji dahulu dan lakukan pembayaran secara manual.
