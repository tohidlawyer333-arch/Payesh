@echo off
set APK=C:\Users\ASUS\Desktop\Payesh\app\build\outputs\apk\release\app-release.apk
echo ===APK FILES===
dir /b "C:\Users\ASUS\Desktop\Payesh\app\build\outputs\apk\release\"
echo ===SIGNATURE VERIFY===
"G:\AndroidSdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs "%APK%"
echo ===BADGING===
"G:\AndroidSdk\build-tools\36.0.0\aapt2.exe" dump badging "%APK%" > "%TEMP%\badging.txt" 2>&1
findstr /i "package application-label versionName versionCode sdkVersion targetSdkVersion" "%TEMP%\badging.txt"
echo ===SIZE===
powershell -NoProfile -Command "(Get-Item '%APK%').Length"
