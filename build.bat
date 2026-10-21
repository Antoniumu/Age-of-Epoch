@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Age of Epoch - build

rem ======================================================================
rem  Age of Epoch - build helper (Minecraft 26.3 / Fabric)
rem
rem    build.bat                assemble the mod  (gradlew build)
rem    build.bat clean          clean, then assemble
rem    build.bat runClient      launch the development client
rem    build.bat getjdk [temurin^|microsoft]  install and check JDK 25
rem    build.bat repair         wipe Loom/Minecraft caches, then rebuild from scratch
rem    build.bat diagnose       write build-diagnose.txt (why compileJava cannot see net.minecraft)
rem    build.bat probe          reproduce Loom's merge step and show the exception it swallows
rem    build.bat singlejar [c]  build without Loom's client+server merge (client or server jar only)
rem    build.bat jcheck         compare JDKs: is Files.isWritable telling the truth?
rem    build.bat <task> ...     any other Gradle task is passed through
rem
rem  Environment (all optional):
rem    JAVA_HOME   JDK 25+; ignored if old or failing the writability check
rem    AOE_JDK_URL  trusted HTTPS JDK ZIP URL; overrides getjdk provider
rem    NOPAUSE=1   do not wait for a key press at the end (for scripts)
rem ======================================================================

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

rem Gradle caches stay inside the project by default (see .gitignore)
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%PROJECT_DIR%\.gradle-home"

if /i "%~1"=="getjdk" goto :getjdk
if /i "%~1"=="repair" goto :repair
if /i "%~1"=="diagnose" goto :diagnose
if /i "%~1"=="probe" goto :probe
if /i "%~1"=="singlejar" goto :singlejar
if /i "%~1"=="jcheck" goto :jcheck

set "ORIGINAL_JAVA_HOME=%JAVA_HOME%"
call :find_jdk
if not defined JAVA_HOME goto :no_jdk

echo.
echo   JDK    : %JAVA_HOME%  (Java %JAVA_MAJOR%)
echo   Caches : %GRADLE_USER_HOME%
echo.

call :jdk_sanity

set "TASKS=%*"
if "%TASKS%"=="" set "TASKS=build"

echo   gradlew %TASKS%
echo.
rem --no-problems-report: the HTML diagnostics report is not needed and is the only
rem Gradle output that can fail on a read-only/locked build directory.
call "%PROJECT_DIR%\gradlew.bat" %TASKS% --no-problems-report
set "RC=%ERRORLEVEL%"

if not "%RC%"=="0" goto :failed

call :list_jar
echo.
echo   BUILD SUCCESSFUL
goto :done

rem ----------------------------------------------------------------------
:failed
echo.
echo   BUILD FAILED (exit code %RC%)
if not "%JAVA_MAJOR%"=="" if %JAVA_MAJOR% LSS 25 echo   Note: the build used Java %JAVA_MAJOR%, Minecraft 26.3 needs Java 25+.
echo   Full log: run the same command with more output, e.g.
echo       build.bat build --stacktrace
echo.
echo   Errors like "package net.minecraft.* does not exist" (with a NullPointerException
echo   in TypeElement.getNestingKind) mean the Minecraft jar on the classpath is empty,
echo   not that the sources are wrong:
echo       build.bat diagnose    what Loom produced, step by step
echo       build.bat probe       why the merge produced nothing (shows the hidden error)
echo       build.bat repair      wipe the caches and rebuild
echo       build.bat jcheck      is this JDK's Files.isWritable lying? (zipfs read-only)
goto :done

rem ----------------------------------------------------------------------
:no_jdk
echo.
echo   No build-ready JDK 25+ found. Run build.bat jcheck for details.
echo.
if defined ORIGINAL_JAVA_HOME echo   JAVA_HOME points to "%ORIGINAL_JAVA_HOME%", which was not usable for this build.
if defined ORIGINAL_JAVA_HOME echo.
echo   Minecraft 26.3 needs Java 25 (Gradle 9.7 + Loom 1.18 refuse older ones).
echo   Pick one of these:
echo.
echo     1) build.bat getjdk      download Temurin 25 into .jdks\ (about 140 MB)
echo     2) install Temurin 25 and set JAVA_HOME to it
echo     3) drop an existing JDK 25 into .jdks\ or %%USERPROFILE%%\.jdks\
echo.
set "RC=1"
goto :done

rem ----------------------------------------------------------------------
:getjdk
if defined AOE_JDK_URL goto :getjdk_url
if "%~2"=="" goto :getjdk_temurin
if /i "%~2"=="temurin" goto :getjdk_temurin
if /i not "%~2"=="microsoft" (
    echo   Usage: build.bat getjdk [temurin^|microsoft]
    set "RC=2"
    goto :done
)
winget install --id Microsoft.OpenJDK.25 -e
if errorlevel 1 (
    echo   Installation failed. Install JDK 25 manually or set AOE_JDK_URL to a ZIP URL.
    set "RC=1"
    goto :done
)
goto :getjdk_verify

:getjdk_temurin
set "AOE_JDK_URL=https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse"
:getjdk_url
echo   Downloading JDK ZIP into "%PROJECT_DIR%\.jdks" ...
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%\tools\install-jdk.ps1" -ProjectDir "%PROJECT_DIR%"
if errorlevel 1 (
    set "RC=1"
    goto :done
)
:getjdk_verify
echo   Checking installed JDKs ...
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%\tools\check-jdks.ps1" -ProjectDir "%PROJECT_DIR%"
call :find_jdk
if not defined JAVA_HOME goto :no_jdk
echo   Build-ready JDK: %JAVA_HOME%
set "RC=0"
goto :done

rem ----------------------------------------------------------------------
:repair
rem  Removes the caches whose corruption makes javac lose the Minecraft
rem  classpath ("package net.minecraft.* does not exist" in every file,
rem  followed by a NullPointerException in the mixin annotation processor).
rem ----------------------------------------------------------------------
echo.
echo   Repairing the build caches ...
echo.
call :find_jdk
if not defined JAVA_HOME goto :no_jdk

echo   JDK    : %JAVA_HOME%  (Java %JAVA_MAJOR%)
echo   Caches : %GRADLE_USER_HOME%
echo.
call :jdk_sanity
call :wipe "%PROJECT_DIR%\.gradle\loom-cache"
call :wipe "%GRADLE_USER_HOME%\caches\fabric-loom"
call :wipe "%GRADLE_USER_HOME%\caches\modules-2\files-2.1\com.mojang"
call :wipe "%GRADLE_USER_HOME%\caches\modules-2\files-2.1\net.minecraft"
call :wipe "%GRADLE_USER_HOME%\caches\modules-2\files-2.1\net.fabricmc"
echo.
echo   gradlew build --refresh-dependencies
echo.
call "%PROJECT_DIR%\gradlew.bat" build --refresh-dependencies --no-problems-report
set "RC=%ERRORLEVEL%"

if not "%RC%"=="0" goto :failed

call :list_jar
echo.
echo   BUILD SUCCESSFUL
goto :done

rem ----------------------------------------------------------------------
rem  %1 = directory to delete (silently skipped when it does not exist)
rem ----------------------------------------------------------------------
:wipe
if not exist "%~1" goto :eof
echo   Removing %~1
rmdir /s /q "%~1"
goto :eof

rem ----------------------------------------------------------------------
:diagnose
rem  Writes build-diagnose.txt: JDK/Gradle info, Loom cache content and the
rem  resolved compile classpath, so it is obvious whether Minecraft is there.
rem ----------------------------------------------------------------------
echo.
echo   Collecting build diagnostics ...
echo.
call :find_jdk
if defined JAVA_HOME echo   JDK    : %JAVA_HOME%  [Java %JAVA_MAJOR%]
if not defined JAVA_HOME echo   JDK    : JDK 25 not found
echo   Caches : %GRADLE_USER_HOME%
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%\tools\diagnose-build.ps1" -ProjectDir "%PROJECT_DIR%"
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
    echo.
    echo   Diagnose failed. Run it manually to see the error:
    echo       powershell -ExecutionPolicy Bypass -File tools\diagnose-build.ps1
)
goto :done

rem ----------------------------------------------------------------------
:jcheck
rem  Runs tools\WritableCheck.java with every JDK that can be found. Loom's
rem  zipfs (JDK 25) marks a jar read-only as soon as Files.isWritable() says
rem  false, so a JDK that misreports it cannot produce minecraft-merged.jar.
rem ----------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%\tools\check-jdks.ps1" -ProjectDir "%PROJECT_DIR%"
set "RC=%ERRORLEVEL%"
goto :done

rem ----------------------------------------------------------------------
:singlejar
rem  Builds with only one Minecraft jar, skipping the client+server merge -
rem  but jar processors still need a writable zip filesystem. This does not
rem  work around a broken Files.isWritable check.
rem  Development-only workaround: the other side's classes are not available.
rem ----------------------------------------------------------------------
set "SIDE=%~2"
if "%SIDE%"=="" set "SIDE=client"
if /i not "%SIDE%"=="client" if /i not "%SIDE%"=="server" (
    echo.
    echo   Usage: build.bat singlejar [client^|server]
    set "RC=2"
    goto :done
)
call :find_jdk
if not defined JAVA_HOME goto :no_jdk

echo.
echo   JDK    : %JAVA_HOME%  [Java %JAVA_MAJOR%]
echo   Caches : %GRADLE_USER_HOME%
echo.
echo   Minecraft jar: %SIDE% only (client+server merge skipped)
echo   gradlew build -PmcSingleJar=%SIDE%
echo.
call :jdk_sanity
call "%PROJECT_DIR%\gradlew.bat" build -PmcSingleJar=%SIDE% --no-problems-report
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" goto :failed
call :list_jar
echo.
echo   BUILD SUCCESSFUL
goto :done

rem ----------------------------------------------------------------------
:probe
rem  Runs tools\MergeProbe.java with the project JDK 25. It performs the same
rem  zip/zipfs operations Loom's MinecraftJarMerger does, but prints the
rem  exception Loom throws away - that is why minecraft-merged.jar comes out
rem  empty (22 bytes) without a single line of explanation.
rem ----------------------------------------------------------------------
echo.
echo   Probing the Loom merge step ...
echo.
call :find_jdk
if not defined JAVA_HOME goto :no_jdk
echo   JDK    : %JAVA_HOME%  [Java %JAVA_MAJOR%]
echo.
"%JAVA_HOME%\bin\java.exe" -Dprobe.projectDir="%PROJECT_DIR%" "%PROJECT_DIR%\tools\MergeProbe.java"
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" echo   The failure above is the reason minecraft-merged.jar is empty - Loom hides it.
if not "%RC%"=="0" echo   If section 5 shows Files.isWritable = false while writes succeed, the JDK
if not "%RC%"=="0" echo   is misreporting it - compare JDKs with:  build.bat jcheck
goto :done

rem ----------------------------------------------------------------------
rem  Warn before a build that cannot work: JDK 25's zipfs trusts
rem  Files.isWritable(), and on some JDK builds that value is wrong for paths
rem  that are perfectly writable. Loom then writes nothing and hides the error.
rem  tools\WritableCheck.java exits with 3 in exactly that case.
rem ----------------------------------------------------------------------
:jdk_sanity
"%JAVA_HOME%\bin\java.exe" "%PROJECT_DIR%\tools\WritableCheck.java" "%PROJECT_DIR%" "%TEMP%" >nul 2>&1
set "WC=%ERRORLEVEL%"
if not "%WC%"=="3" goto :eof
echo.
echo   [warning] This JDK reports Files.isWritable = false for paths it can write to.
echo             JDK 25 zipfs then declares every jar read-only, Loom's merge writes
echo             nothing and you get an empty minecraft-merged.jar and bogus
echo             "package net.minecraft.* does not exist" errors.
echo             Compare the JDKs on this machine:   build.bat jcheck
echo.
goto :eof

rem ----------------------------------------------------------------------
rem  Locate a JDK 25+: JAVA_HOME first, then the usual install directories.
rem  Unsets JAVA_HOME when nothing suitable is found, so the caller cannot
rem  silently build with an older JDK.
rem ----------------------------------------------------------------------
:find_jdk
set "FOUND_JDK="
set "FOUND_MAJOR="
set "JAVA_MAJOR="
if defined JAVA_HOME call :try_jdk "%JAVA_HOME%"
if not defined FOUND_JDK for /d %%d in ("%PROJECT_DIR%\.jdks\*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%USERPROFILE%\.jdks\*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%ProgramFiles%\Java\jdk-*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%ProgramFiles%\Microsoft\jdk-*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%ProgramFiles%\Zulu\zulu-*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%ProgramFiles%\Amazon Corretto\jdk*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%ProgramFiles%\BellSoft\LibericaJDK-*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-*") do call :try_jdk "%%~fd"
if not defined FOUND_JDK for /d %%d in ("%LOCALAPPDATA%\Programs\Microsoft\jdk-*") do call :try_jdk "%%~fd"
if defined FOUND_JDK (
    set "JAVA_HOME=%FOUND_JDK%"
    set "JAVA_MAJOR=%FOUND_MAJOR%"
) else (
    set "JAVA_HOME="
)
goto :eof

rem ----------------------------------------------------------------------
rem  %1 = candidate JDK home; keeps it if its major version is 25 or newer
rem ----------------------------------------------------------------------
:try_jdk
if defined FOUND_JDK goto :eof
set "CAND=%~1"
if "%CAND:~-1%"=="\" set "CAND=%CAND:~0,-1%"
if not exist "%CAND%\bin\javac.exe" goto :eof
if not exist "%CAND%\release" goto :eof
set "CAND_MAJOR="
set "CAND_VER="
for /f "tokens=2 delims== " %%v in ('findstr /b "JAVA_VERSION=" "%CAND%\release"') do set "CAND_VER=%%~v"
if not defined CAND_VER goto :eof
for /f "tokens=1 delims=._-+ " %%m in ("%CAND_VER%") do set "CAND_MAJOR=%%m"
if not defined CAND_MAJOR goto :eof
if %CAND_MAJOR% LSS 25 goto :eof
rem Check actual build locations, not TEMP: a failure there is reported by jcheck.
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"
"%CAND%\bin\java.exe" "%PROJECT_DIR%\tools\WritableCheck.java" "%PROJECT_DIR%" "%GRADLE_USER_HOME%" >nul 2>&1
set "CAND_CHECK=%ERRORLEVEL%"
if not "%CAND_CHECK%"=="0" (
    echo   Skipping JDK: %CAND% [writability check exit %CAND_CHECK%; run build.bat jcheck]
    goto :eof
)
set "FOUND_JDK=%CAND%"
set "FOUND_MAJOR=%CAND_MAJOR%"
goto :eof

rem ----------------------------------------------------------------------
rem  Print the freshly built mod jar (newest, excluding -sources)
rem ----------------------------------------------------------------------
:list_jar
set "JAR="
for /f "delims=" %%f in ('dir /b /o-d "%PROJECT_DIR%\build\libs\*.jar" 2^>nul ^| findstr /i /v "sources"') do (
    if not defined JAR set "JAR=%%f"
)
if not defined JAR goto :eof
echo.
echo   Mod jar: %PROJECT_DIR%\build\libs\%JAR%
for %%s in ("%PROJECT_DIR%\build\libs\%JAR%") do echo   Size   : %%~zs bytes
goto :eof

rem ----------------------------------------------------------------------
:done
if not defined NOPAUSE (
    echo %cmdcmdline% | find /i "%~nx0" >nul && pause
)
exit /b %RC%
