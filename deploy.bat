@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Age of Epoch - deploy

rem ======================================================================
rem  Copies the freshly built mod jar into the ElyPrism instance and,
rem  with "launch", starts that instance through the launcher CLI.
rem
rem    deploy.bat            copy the jar, keep the game closed
rem    deploy.bat launch     copy the jar and start the instance
rem
rem  Overridable environment variables:
rem    AOE_INSTANCE   instance id            (default: 26.3)
rem    AOE_PROFILE    launcher account name  (default: Antoniumu)
rem    AOE_MODS_DIR   mods folder            (default: <instance>\minecraft\mods)
rem    AOE_LAUNCHER   launcher executable
rem ======================================================================

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

if not defined AOE_INSTANCE set "AOE_INSTANCE=26.3"
if not defined AOE_PROFILE set "AOE_PROFILE=Antoniumu"
if not defined AOE_MODS_DIR set "AOE_MODS_DIR=%APPDATA%\ElyPrismLauncher\instances\%AOE_INSTANCE%\minecraft\mods"
if not defined AOE_LAUNCHER set "AOE_LAUNCHER=%LOCALAPPDATA%\Programs\ElyPrismLauncher\ElyPrismLauncher.exe"

set "JAR="
for /f "delims=" %%f in ('dir /b /o-d "%PROJECT_DIR%\build\libs\*.jar" 2^>nul ^| findstr /i /v "sources"') do (
    if not defined JAR set "JAR=%%f"
)

if not defined JAR (
    echo   No built jar in "%PROJECT_DIR%\build\libs" - run build.bat first.
    exit /b 1
)
if not exist "%AOE_MODS_DIR%" (
    echo   Mods folder not found: "%AOE_MODS_DIR%"
    exit /b 1
)

echo   Jar      : %JAR%
echo   Instance : %AOE_INSTANCE%
echo   Mods dir : %AOE_MODS_DIR%

rem remove older copies of this mod, then drop the new one in
del /q "%AOE_MODS_DIR%\Age-of-Epoch-*.jar" >nul 2>&1
copy /y "%PROJECT_DIR%\build\libs\%JAR%" "%AOE_MODS_DIR%\" >nul 2>&1
if errorlevel 1 (
    echo.
    echo   Copy failed. If the game is running, close it and try again.
    exit /b 1
)
echo   Copied   : %AOE_MODS_DIR%\%JAR%

if /i not "%~1"=="launch" (
    echo   Launch   : skipped ^(pass "launch" to start the instance^)
    exit /b 0
)

if not exist "%AOE_LAUNCHER%" (
    echo   Launcher not found: "%AOE_LAUNCHER%"
    echo   Set AOE_LAUNCHER to the ElyPrismLauncher.exe path.
    exit /b 1
)

echo   Launch   : %AOE_INSTANCE% with profile %AOE_PROFILE%
start "" "%AOE_LAUNCHER%" --launch "%AOE_INSTANCE%" --profile "%AOE_PROFILE%"
exit /b 0
