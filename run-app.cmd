@echo off
setlocal
pushd "%~dp0"
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
set "APP_ADB="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "APP_ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if not defined APP_ADB if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" set "APP_ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
if not defined APP_ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "APP_ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined APP_ADB (
    echo Android SDK not found. Set ANDROID_HOME to the SDK directory and retry.
    goto :failed
)
"%APP_ADB%" -e get-state
if errorlevel 1 (
    echo Start exactly one Pleos emulator before running this script.
    goto :failed
)
call gradlew.bat assembleDebug
if errorlevel 1 goto :failed
"%APP_ADB%" -e install -r "app\build\outputs\apk\debug\app-debug.apk"
if errorlevel 1 goto :failed
"%APP_ADB%" -e shell am force-stop com.example.hyundiaboysranger
if errorlevel 1 goto :failed
"%APP_ADB%" -e shell am start -W -n com.example.hyundiaboysranger/.MainActivity
if errorlevel 1 goto :failed
echo App launch requested. Check the Pleos screen.
popd
exit /b 0
:failed
echo Stopped. Read the error above. Existing app data has not been cleared.
popd
exit /b 1
