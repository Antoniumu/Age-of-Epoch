# ---------------------------------------------------------------------------
#  Age of Epoch - build diagnostics
#
#  Answers one question: is Minecraft actually on the compile classpath?
#
#  It is written for this failure mode:
#      error: package net.minecraft.network.chat does not exist
#      ...
#      NullPointerException: Cannot invoke
#      "javax.lang.model.element.TypeElement.getNestingKind()" because "typeElement" is null
#
#  That pair means Loom's Minecraft jar is missing/empty, not that the sources
#  are wrong.  The script prints a verdict and writes build-diagnose.txt.
#
#  Usage (from the project root):
#      powershell -ExecutionPolicy Bypass -File tools\diagnose-build.ps1
#  or simply:
#      build.bat diagnose
# ---------------------------------------------------------------------------
[CmdletBinding()]
param(
    [string]$ProjectDir = ''
)

if ([string]::IsNullOrEmpty($ProjectDir)) {
    $ProjectDir = Split-Path -Parent $PSScriptRoot
}
$ProjectDir = (Resolve-Path $ProjectDir).Path
$ReportPath = Join-Path $ProjectDir 'build-diagnose.txt'

$script:Lines = @()

function Say {
    param([string]$Text = '')
    $script:Lines += $Text
    Write-Host $Text
}

function Section {
    param([string]$Title)
    Say ''
    Say ('=== ' + $Title + ' ' + ('=' * [Math]::Max(3, 60 - $Title.Length)))
}

function Size-Mb {
    param([long]$Bytes)
    return ('{0:N1} MB' -f ($Bytes / 1MB))
}

Say ('Age of Epoch - build diagnostics, ' + (Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))

$gradleHome = $env:GRADLE_USER_HOME
if ([string]::IsNullOrEmpty($gradleHome)) {
    $gradleHome = Join-Path $env:USERPROFILE '.gradle'
}

Section 'Environment'
Say ('Project          : ' + $ProjectDir)
Say ('GRADLE_USER_HOME : ' + $gradleHome)
Say ('JAVA_HOME        : ' + $env:JAVA_HOME)
if (Get-Command java -ErrorAction SilentlyContinue) {
    Say ('java -version    : ' + ((& java -version 2>&1) -join ' | '))
} else {
    Say 'java -version    : java not found on PATH'
}

$propsFile = Join-Path $ProjectDir 'gradle.properties'
if (Test-Path $propsFile) {
    Section 'gradle.properties'
    foreach ($line in (Get-Content $propsFile)) {
        if ($line -match '=' -and $line -notmatch '^\s*#') {
            Say ('  ' + $line)
        }
    }
}

Section 'Loom cache jars (project .gradle\loom-cache)'
$loomCache = Join-Path $ProjectDir '.gradle\loom-cache'
$loomJars = @()
if (Test-Path $loomCache) {
    $loomJars = @(Get-ChildItem -Path $loomCache -Recurse -Filter *.jar -File -ErrorAction SilentlyContinue)
}
if ($loomJars.Count -eq 0) {
    Say '  [!!] no jars at all - Loom has not produced a Minecraft jar here'
} else {
    foreach ($jar in ($loomJars | Sort-Object -Property FullName)) {
        Say ('  ' + $jar.FullName.Substring($ProjectDir.Length).TrimStart('\') + '  ' + (Size-Mb $jar.Length))
    }
}

Section 'Resolved compile classpath (gradle dshClasspath)'
$gradlew = Join-Path $ProjectDir 'gradlew.bat'
$cpFile = Join-Path $ProjectDir 'build\dsh-cp.txt'
if (Test-Path $gradlew) {
    Push-Location $ProjectDir
    try {
        & $gradlew -q dshClasspath --no-problems-report 2>&1 | ForEach-Object { Say ('  ' + $_) }
    } finally {
        Pop-Location
    }
} else {
    Say '  [!!] gradlew.bat not found - cannot resolve the classpath'
}

$mcEntries = @()
if (Test-Path $cpFile) {
    $raw = Get-Content $cpFile -Raw
    $all = @($raw -split ';' | Where-Object { $_ -ne '' })
    Say ('  classpath entries           : ' + $all.Count)
    $mcEntries = @($all | Where-Object { $_ -match '(?i)minecraft' })
    Say ('  entries matching minecraft  : ' + $mcEntries.Count)
    foreach ($entry in $mcEntries) {
        Say ('    ' + $entry)
    }
} else {
    Say '  [!!] build\dsh-cp.txt was not produced'
}

Section 'Minecraft jar contents'
Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
$haveMcClasses = $false
foreach ($entry in $mcEntries) {
    if (-not (Test-Path $entry)) {
        Say ('  [!!] listed on the classpath but missing on disk: ' + $entry)
        continue
    }
    $file = Get-Item $entry
    Say ('  ' + $file.Name + '  ' + (Size-Mb $file.Length))
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($entry)
        try {
            $count = 0
            foreach ($item in $zip.Entries) {
                if ($item.FullName.StartsWith('net/minecraft/')) { $count++ }
            }
            Say ('    net/minecraft/ entries : ' + $count)
            foreach ($probe in @('net/minecraft/resources/Identifier.class',
                                 'net/minecraft/ChatFormatting.class',
                                 'net/minecraft/server/MinecraftServer.class')) {
                $hit = ($null -ne $zip.GetEntry($probe))
                if ($hit) { $haveMcClasses = $true }
                Say ('    ' + $probe + ' -> ' + $hit)
            }
        } finally {
            $zip.Dispose()
        }
    } catch {
        Say ('    [!!] cannot open the jar: ' + $_.Exception.Message)
    }
}

Section 'Gradle artifact cache'
$files21 = Join-Path $gradleHome 'caches\modules-2\files-2.1'
foreach ($group in @('com.mojang', 'net.minecraft')) {
    $groupDir = Join-Path $files21 $group
    if (Test-Path $groupDir) {
        Say ('  ' + $groupDir)
        foreach ($item in (Get-ChildItem -Path $groupDir -Recurse -File -ErrorAction SilentlyContinue)) {
            Say ('    ' + $item.FullName.Substring($groupDir.Length).TrimStart('\') + '  ' + $item.Length + ' B')
        }
    } else {
        Say ('  [!!] not cached: ' + $groupDir)
    }
}
$loomHome = Join-Path $gradleHome 'caches\fabric-loom'
if (Test-Path $loomHome) {
    Say ('  [ok] ' + $loomHome)
} else {
    Say ('  [!!] not cached: ' + $loomHome)
}

Section 'Verdict'
if ($mcEntries.Count -eq 0) {
    Say '  [!!] Minecraft is NOT on the compile classpath.'
    Say '       The "package net.minecraft.* does not exist" errors and the mixin'
    Say '       NullPointerException come from that, not from the sources.'
    Say '       Fix: build.bat repair'
} elseif (-not $haveMcClasses) {
    Say '  [!!] A Minecraft jar is on the classpath but it holds no net/minecraft'
    Say '       classes - the jar is empty or truncated (interrupted remap, full disk,'
    Say '       antivirus).  Fix: build.bat repair'
} else {
    Say '  [OK] Minecraft classes are on the compile classpath.'
    Say '       If compileJava still fails, the errors are source-level.  Get them all:'
    Say '           gradlew compileJava -PdshMaxErrs=4000'
}

try {
    Set-Content -Path $ReportPath -Value $script:Lines -Encoding UTF8
    Say ''
    Say ('Report written to ' + $ReportPath)
} catch {
    Write-Host ('Could not write ' + $ReportPath + ': ' + $_.Exception.Message)
}

exit 0
