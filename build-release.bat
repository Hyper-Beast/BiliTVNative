@echo off
setlocal EnableExtensions

cd /d "%~dp0"

if "%ANDROID_HOME%"=="" (
  set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)
if "%ANDROID_SDK_ROOT%"=="" (
  set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
)

set "BUILD_ROOT=%USERPROFILE%\.gradle\bilitv-native-build"
set "SOURCE_APK=%BUILD_ROOT%\app\outputs\apk\release\app-release.apk"
set "OUTPUT_DIR=%BUILD_ROOT%\release-apks"

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo Building BiliTVNative release APKs...
echo Android SDK: %ANDROID_HOME%
echo Output directory: %OUTPUT_DIR%
echo.

if "%~1"=="" (
  call :build_abi armeabi-v7a
  if errorlevel 1 goto failed

  call :build_abi arm64-v8a
  if errorlevel 1 goto failed
) else (
  call :build_abi "%~1"
  if errorlevel 1 goto failed
)

echo.
echo Release APKs are ready:
if exist "%OUTPUT_DIR%\BiliTVNative-armeabi-v7a-release.apk" echo   %OUTPUT_DIR%\BiliTVNative-armeabi-v7a-release.apk
if exist "%OUTPUT_DIR%\BiliTVNative-arm64-v8a-release.apk" echo   %OUTPUT_DIR%\BiliTVNative-arm64-v8a-release.apk
if not "%CI%"=="1" pause
exit /b 0

:build_abi
set "TARGET_ABI=%~1"

if /i not "%TARGET_ABI%"=="armeabi-v7a" if /i not "%TARGET_ABI%"=="arm64-v8a" (
  echo Unsupported release ABI: %TARGET_ABI%
  echo Supported values: armeabi-v7a, arm64-v8a
  exit /b 1
)

echo Building target ABI: %TARGET_ABI%
call "%~dp0gradlew.bat" :app:assembleRelease -PtargetAbi=%TARGET_ABI%
if errorlevel 1 exit /b 1

if not exist "%SOURCE_APK%" (
  echo Release APK was not generated: %SOURCE_APK%
  exit /b 1
)

set "OUTPUT_APK=%OUTPUT_DIR%\BiliTVNative-%TARGET_ABI%-release.apk"
copy /y "%SOURCE_APK%" "%OUTPUT_APK%" >nul
if errorlevel 1 (
  echo Failed to copy release APK to: %OUTPUT_APK%
  exit /b 1
)

echo Created: %OUTPUT_APK%
echo.
exit /b 0

:failed
echo.
echo Build failed.
if not "%CI%"=="1" pause
exit /b 1
