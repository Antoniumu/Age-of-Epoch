@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title Age of Epoch - git auto-pull + build

rem ======================================================================
rem  Watches the git upstream, pulls new commits and rebuilds the mod.
rem
rem    watch.bat            poll every 60 seconds
rem    watch.bat 30         poll every 30 seconds
rem    watch.bat 0          poll as fast as possible (not recommended)
rem
rem  After every successful build the jar is copied into the ElyPrism
rem  instance and the instance is started through the launcher CLI
rem  (see deploy.bat for the instance/profile/variables).
rem  Set AOE_NO_LAUNCH=1 to only deploy without starting the game.
rem  Set AOE_NO_DEPLOY=1 to only build.
rem
rem  Ctrl+C stops it. Everything is appended to autobuild.log.
rem  Builds run with NOPAUSE=1 so the watcher never waits for a key press.
rem ======================================================================

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"
set "LOG=%PROJECT_DIR%\autobuild.log"

set "INTERVAL=%~1"
if "%INTERVAL%"=="" set "INTERVAL=60"

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 goto :no_git

git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >nul 2>&1
if errorlevel 1 goto :no_upstream

for /f %%b in ('git rev-parse --abbrev-ref HEAD') do set "BRANCH=%%b"
for /f %%r in ('git config --get branch.%BRANCH%.remote') do set "REMOTE_NAME=%%r"

echo.
echo   Repo     : %PROJECT_DIR%
echo   Branch   : %BRANCH% (tracking %REMOTE_NAME%)
echo   Interval : %INTERVAL% s
echo   Log      : %LOG%
echo   Ctrl+C to stop.
echo.
call :log "watcher started: branch %BRANCH%, interval %INTERVAL%s"

:loop
call :tick
if "%INTERVAL%"=="0" goto :loop
call :sleep %INTERVAL%
goto :loop

rem ----------------------------------------------------------------------
rem  One poll: fetch, fast-forward when possible, rebuild when HEAD moved
rem ----------------------------------------------------------------------
:tick
set "NEED_BUILD="
set "LOCAL="
set "BEHIND="
set "FERR="
for /f %%h in ('git rev-parse HEAD') do set "LOCAL=%%h"

git fetch --quiet 2>"%TEMP%\aoe-fetch-err.txt" || goto :fetch_failed
set "LAST_FERR="

for /f %%n in ('git rev-list --count "HEAD..@{u}"') do set "BEHIND=%%n"
if "%BEHIND%"=="" set "BEHIND=0"
if not "%BEHIND%"=="0" goto :pull

rem nothing new upstream: rebuild only if HEAD is not the commit we last built
if "%LOCAL%"=="%LAST_BUILT%" goto :up_to_date
if not defined LAST_BUILT goto :initial_build
echo   [%TIME%] local commit %LOCAL:~0,7%
call :log "local commit %LOCAL:~0,7%, rebuilding"
set "NEED_BUILD=1"
goto :maybe_build

:initial_build
echo   [%TIME%] initial build (%LOCAL:~0,7%)
call :log "initial build %LOCAL:~0,7%"
set "NEED_BUILD=1"
goto :maybe_build

:pull
for /f %%h in ('git rev-parse "@{u}"') do set "REMOTE=%%h"
echo   [%TIME%] new commits upstream: %LOCAL:~0,7% -^> %REMOTE:~0,7% (%BEHIND%)
call :log "new commit(s) upstream: %LOCAL:~0,7% -> %REMOTE:~0,7% (%BEHIND%)"
git pull --ff-only --quiet
if errorlevel 1 (
    echo   [%TIME%] pull failed - local changes or diverged history, resolve manually
    call :log "pull --ff-only failed; skipping until the working tree is clean"
    goto :maybe_build
)
for /f %%h in ('git rev-parse HEAD') do set "LOCAL=%%h"
set "NEED_BUILD=1"
goto :maybe_build

:up_to_date
echo   [%TIME%] up to date (%LOCAL:~0,7%)
goto :tick_end

:fetch_failed
for /f "usebackq delims=" %%e in ("%TEMP%\aoe-fetch-err.txt") do if not defined FERR set "FERR=%%e"
if not defined FERR set "FERR=unknown error"
if not "%FERR%"=="%LAST_FERR%" (
    set "LAST_FERR=%FERR%"
    echo   [%TIME%] git fetch failed: %FERR%
    call :log "git fetch failed: %FERR%"
)
rem keep the watcher useful offline: build the current HEAD once
if not defined LAST_BUILT set "NEED_BUILD=1"

:maybe_build
if not defined NEED_BUILD goto :tick_end
call :build

:tick_end
goto :eof

rem ----------------------------------------------------------------------
rem  Build through build.bat, mirror its output to the console and the log
rem ----------------------------------------------------------------------
:build
echo.
echo   ============================================================
echo   [%TIME%] building %LOCAL:~0,7%
echo   ============================================================
call :log "building %LOCAL:~0,7%"
set "OUT=%TEMP%\aoe-watch-build.txt"
set "NOPAUSE=1"
call "%PROJECT_DIR%\build.bat" build > "%OUT%" 2>&1
set "RC=%ERRORLEVEL%"
set "NOPAUSE="
type "%OUT%"
call :log "build finished with exit code %RC%"
for /f "delims=" %%j in ('findstr /c:"Mod jar:" "%OUT%" 2^>nul') do call :log "%%j"
del "%OUT%" >nul 2>&1
if not "%RC%"=="0" (
    echo   [%TIME%] BUILD FAILED - see autobuild.log
    goto :eof
)
echo   [%TIME%] BUILD OK
if defined LOCAL set "LAST_BUILT=%LOCAL%"
call :deploy
goto :eof

rem ----------------------------------------------------------------------
rem  Copy the jar into the ElyPrism instance and start it (unless AOE_NO_LAUNCH)
rem ----------------------------------------------------------------------
:deploy
if defined AOE_NO_DEPLOY (
    echo   [%TIME%] deploy skipped ^(AOE_NO_DEPLOY^)
    call :log "deploy skipped: AOE_NO_DEPLOY"
    goto :eof
)
echo.
if defined AOE_NO_LAUNCH (
    call "%PROJECT_DIR%\deploy.bat"
) else (
    call "%PROJECT_DIR%\deploy.bat" launch
)
if errorlevel 1 (
    echo   [%TIME%] deploy failed - instance not updated
    call :log "deploy failed"
    goto :eof
)
if defined AOE_NO_LAUNCH (
    call :log "mod deployed to the instance (launch skipped: AOE_NO_LAUNCH)"
) else (
    call :log "mod deployed and instance %AOE_INSTANCE% launched"
)
goto :eof

rem ----------------------------------------------------------------------
:sleep
for /l %%i in (1,1,%1) do ping -n 2 127.0.0.1 >nul
goto :eof

rem ----------------------------------------------------------------------
:log
>>"%LOG%" echo [%DATE% %TIME%] %~1
goto :eof

rem ----------------------------------------------------------------------
:no_git
echo.
echo   This folder is not a git repository: %PROJECT_DIR%
set "RC=1"
goto :done

:no_upstream
echo.
echo   The current branch has no upstream configured, nothing to pull.
echo   Set one first, for example:
echo       git branch --set-upstream-to=origin/main main
set "RC=1"
goto :done

:done
if not defined NOPAUSE (
    echo %cmdcmdline% | find /i "%~nx0" >nul && pause
)
exit /b %RC%
