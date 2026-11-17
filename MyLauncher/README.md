# MyLauncher v0.2

Launcher Minecraft Java untuk Android (Kotlin + C/JNI).

## Sudah lengkap di kode ini
- Daftar versi, download client/libraries/assets, evaluasi rules Mojang
- Login offline (UUID offline), pengaturan RAM dan renderer
- Ekstrak JRE dan LWJGL dari assets (zip)
- JNI (C): dlopen libjvm.so -> JNI_CreateJavaVM -> panggil main()
- ANativeWindow dari SurfaceView (siap dipakai lapisan GLFW)
- Game jalan di proses terpisah (:game)
- Build APK lewat GitHub Actions

## Yang harus kamu tambahkan (file biner, tidak bisa ditulis dari nol)
1. app/src/main/assets/jre-arm64.zip       (OpenJDK 17 Android arm64)
2. app/src/main/assets/lwjgl-android.zip   (jar LWJGL 3 untuk Android)
3. app/src/main/jniLibs/arm64-v8a/*.so     (libglfw, liblwjgl*, renderer gl4es/zink/angle)
Semua ini ada di PojavLauncher / ZalithLauncher (LGPL-3.0).

## Yang belum ada
- Kontrol sentuh (overlay tombol), login Microsoft, argumen game bersyarat

## Build
Push ke GitHub -> tab Actions -> unduh artifact APK.
