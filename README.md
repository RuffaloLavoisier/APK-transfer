# APK-transfer

This project enables APK transfer from an Android device to a PC using only an Android app and a PC client, without requiring ADB, cable, or root access. It consists of two components: the APK-transfer-APP (Android application) and the APK-transfer-SERVER (PC server). The app extracts APK files and sends them to the server via HTTP.

## Components
- **APK-transfer-APP**: The Android application that runs on the device and handles the extraction and transfer of APK files.
- **APK-transfer-SERVER**: The PC server that receives the APK files and provides a web interface for managing the transfers.

## TODO

- [x] send installed apk file to PC
- [x] send apk file from explorer to PC
- [ ] improve UI/UX (Sorry, I'm not a designer xD)
- [x] receive apk file on PC
- [ ] send file to device
- [ ] receive file on device
- [x] search installed apps(package name/app name)
- [x] setting page (change server address, port, etc.)
- [ ] clean up code and add comments