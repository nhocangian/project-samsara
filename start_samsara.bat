@echo off
chcp 65001 >nul
title Project Samsara - Dev Client

rem ==========================================================================
rem  Bật Minecraft dev client của mod Project Samsara (Iris + Sodium + shader).
rem  - Tự build lại mod nếu code thay đổi rồi mở game.
rem  - Shader đọc trực tiếp từ shader\Samsara-Cosmic-Shaders (junction), nên sửa
rem    shader xong chỉ cần tải lại shader trong game, không cần chạy lại file này.
rem  - Log ghi vào mod\run\samsara_client.log
rem ==========================================================================

set "JAVA_HOME=C:\Program Files\Zulu\zulu-21"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [LOI] Khong tim thay JDK 21 tai "%JAVA_HOME%".
    pause
    exit /b 1
)

set "GRADLE="
for /d %%D in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.6-bin\*") do (
    if exist "%%D\gradle-8.6\bin\gradle.bat" set "GRADLE=%%D\gradle-8.6\bin\gradle.bat"
)
if not defined GRADLE (
    echo [LOI] Khong tim thay Gradle 8.6 trong %USERPROFILE%\.gradle\wrapper\dists.
    pause
    exit /b 1
)

tasklist /v /fi "imagename eq java.exe" | findstr /i "Minecraft" >nul
if %errorlevel%==0 (
    echo [!] Minecraft dev client dang mo roi. Dong no truoc khi bat lai.
    pause
    exit /b 0
)

cd /d "%~dp0mod"
echo Dang build va mo Minecraft (khoang 30-60 giay)...
echo Log: %~dp0mod\run\samsara_client.log
call "%GRADLE%" runClient --console=plain > "%~dp0mod\run\samsara_client.log" 2>&1
if errorlevel 1 (
    echo [LOI] Game dong voi loi. Xem log: mod\run\samsara_client.log
    pause
)
