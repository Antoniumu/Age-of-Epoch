# ---------------------------------------------------------------------------
#  Age of Epoch - build diagnostics
#
#  Answers one question: is Minecraft really on the compile classpath, and if
#  not, at which step did it disappear?
#
#  It is written for this failure mode:
#      error: package net.minecraft.network.chat does not exist     (every file)
#      ...
#      NullPointerException: Cannot invoke
#      "javax.lang.model.element.TypeElement.getNestingKind()" because "typeElement" is null
#      minecraft-merged-<hash>-26.3.jar (22 bytes)
#
#  A 22 byte jar is an empty zip: the step that writes it produced no entries.
#  The report walks the chain Loom uses and shows which link is empty:
#
#      caches\fabric-loom\26.3\minecraft-client.jar      (download)
#      caches\fabric-loom\26.3\minecraft-server.jar      (download)
#      caches\fabric-loom\26.3\minecraft-merged.jar      (merge of the two)
#      ...\minecraftMaven\net\minecraft\minecraft-merged-<hash>\26.3\*.jar   (remap + jar processors)
#
#  Usage (from the project root):
#      powershell -ExecutionPolicy Bypass -File tools\diagnose-build.ps1
#  or simply:
#      build.bat diagnose
#
#  Switches:
#      -ProjectDir <path>   project root (default: parent of this script)
#      -SkipGradle          do not run "gradlew dshClasspath" (no classpath section)
# ---------------------------------------------------------------------------
[CmdletBinding()]
param(
    [string]$ProjectDir = '',
    [switch]$SkipGradle
)

if ([string]::IsNullOrEmpty($ProjectDir)) {
    $ProjectDir = Split-Path -Parent $PSScriptRoot
}
$ProjectDir = (Resolve-Path $ProjectDir).Path
$ReportPath = Join-Path $ProjectDir 'build-diagnose.txt'

$script:Lines = @()
$script:Facts = @{}   # machine readable probe results, used by the verdict

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

function Mb {
    param([long]$Bytes)
    return ('{0:N1} MB' -f ($Bytes / 1MB))
}

<#
  Inspects one jar: size and how many net/minecraft entries it holds.
  Returns $null when the file cannot be read as a zip.
#>
function Get-JarInfo {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return $null }
    $file = Get-Item $Path
    $info = [pscustomobject]@{
        Path      = $file.FullName
        Size      = $file.Length
        McEntries = -1       # -1 = unreadable / not a zip
        Probe     = $false   # contains a known Minecraft class
    }
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($file.FullName)
        try {
            $count = 0
            foreach ($item in $zip.Entries) {
                if ($item.FullName.StartsWith('net/minecraft/')) { $count++ }
            }
            $info.McEntries = $count
            foreach ($probe in @('net/minecraft/resources/Identifier.class',
                                 'net/minecraft/ChatFormatting.class',
                                 'net/minecraft/server/MinecraftServer.class')) {
                if ($null -ne $zip.GetEntry($probe)) { $info.Probe = $true }
            }
        } finally {
            $zip.Dispose()
        }
    } catch {
        $info.McEntries = -1
    }
    return $info
}

function Show-Jar {
    param($Info)
    if ($null -eq $Info) {
        Say '    (missing)'
        return
    }
    $entries = if ($Info.McEntries -lt 0) { 'not a readable zip' } else { '' + $Info.McEntries + ' net/minecraft entries' }
    Say ('    ' + $Info.Path + '  ' + (Mb $Info.Size) + '  ' + $entries)
}

Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue

Say ('Age of Epoch - build diagnostics, ' + (Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))

$gradleHome = $env:GRADLE_USER_HOME
if ([string]::IsNullOrEmpty($gradleHome)) {
    $gradleHome = Join-Path $env:USERPROFILE '.gradle'
}
$mcVersion = '26.3'
$gradleProps = Join-Path $ProjectDir 'gradle.properties'
if (Test-Path $gradleProps) {
    $hit = Select-String -Path $gradleProps -Pattern '^\s*minecraft_version\s*=\s*(\S+)' | Select-Object -First 1
    if ($null -ne $hit) { $mcVersion = $hit.Matches[0].Groups[1].Value.Trim() }
}

# ---------------------------------------------------------------------------
Section 'Environment'
Say ('Project          : ' + $ProjectDir)
Say ('GRADLE_USER_HOME : ' + $gradleHome)
Say ('JAVA_HOME        : ' + $env:JAVA_HOME)
if (Get-Command java -ErrorAction SilentlyContinue) {
    Say ('java on PATH     : ' + ((& java -version 2>&1) -join ' | '))
} else {
    Say 'java on PATH     : java not found'
}
Say '                   (Gradle and the probe use JAVA_HOME; PATH only matters for tools you run by hand)'
Say ('Minecraft        : ' + $mcVersion)

$drive = (Get-Item $ProjectDir).PSDrive
$freeMb = [math]::Round((Get-PSDrive $drive.Name).Free / 1MB)
$script:Facts['FreeMb'] = $freeMb
Say ('Free space       : ' + $freeMb + ' MB on ' + $drive.Name + ':')
if ($freeMb -lt 1024) {
    Say '  [!!] less than 1 GB free - this alone can make Loom write empty jars'
}

if (Test-Path $gradleProps) {
    Section 'gradle.properties'
    foreach ($line in (Get-Content $gradleProps)) {
        if ($line -match '=' -and $line -notmatch '^\s*#') { Say ('  ' + $line) }
    }
}

# ---------------------------------------------------------------------------
Section ('Downloads and merge (caches\fabric-loom\' + $mcVersion + ')')
$mcDir = Join-Path (Join-Path $gradleHome 'caches\fabric-loom') $mcVersion
$clientJar = Get-JarInfo (Join-Path $mcDir 'minecraft-client.jar')
$serverJar = Get-JarInfo (Join-Path $mcDir 'minecraft-server.jar')
$mergedJar = Get-JarInfo (Join-Path $mcDir 'minecraft-merged.jar')
Say '  client.jar (download)'
Show-Jar $clientJar
Say '  server.jar (download)'
Show-Jar $serverJar
Say '  extracted_server.jar (unpacked bundle, used by newer versions)'
$extractedServerJar = Get-JarInfo (Join-Path $mcDir 'minecraft-extracted_server.jar')
Show-Jar $extractedServerJar
Say '  merged.jar (merge of the two)'
Show-Jar $mergedJar
$script:Facts['ClientEntries'] = if ($null -ne $clientJar) { $clientJar.McEntries } else { -2 }
$script:Facts['ServerEntries'] = if ($null -ne $serverJar) { $serverJar.McEntries } else { -2 }
$script:Facts['ExtractedServerEntries'] = if ($null -ne $extractedServerJar) { $extractedServerJar.McEntries } else { -2 }
$script:Facts['MergedEntries'] = if ($null -ne $mergedJar) { $mergedJar.McEntries } else { -2 }
$script:Facts['ClientSize'] = if ($null -ne $clientJar) { $clientJar.Size } else { 0 }
$script:Facts['ServerSize'] = if ($null -ne $serverJar) { $serverJar.Size } else { 0 }

# ---------------------------------------------------------------------------
Section 'Remapped / processed jars (minecraftMaven)'
$repos = @(
    (Join-Path (Join-Path $gradleHome 'caches\fabric-loom') 'minecraftMaven'),
    (Join-Path $ProjectDir '.gradle\loom-cache\minecraftMaven')
)
$mavenJars = @()
foreach ($repo in $repos) {
    Say ('  repo: ' + $repo)
    if (-not (Test-Path $repo)) {
        Say '    (not created yet)'
        continue
    }
    $jars = @(Get-ChildItem -Path $repo -Recurse -Filter *.jar -File -ErrorAction SilentlyContinue)
    if ($jars.Count -eq 0) {
        Say '    (no jars)'
        continue
    }
    foreach ($jar in ($jars | Sort-Object -Property FullName)) {
        $info = Get-JarInfo $jar.FullName
        $mavenJars += $info
        Show-Jar $info
    }
}
$smallest = $mavenJars | Sort-Object -Property Size | Select-Object -First 1
$script:Facts['MavenSmallestSize'] = if ($null -ne $smallest) { $smallest.Size } else { -1 }
$script:Facts['MavenSmallestEntries'] = if ($null -ne $smallest) { $smallest.McEntries } else { -1 }

# ---------------------------------------------------------------------------
Section 'Other Loom caches'
foreach ($path in @(
        (Join-Path $ProjectDir '.gradle\loom-cache'),
        (Join-Path $gradleHome 'caches\fabric-loom'))) {
    if (Test-Path $path) {
        $files = @(Get-ChildItem -Path $path -Recurse -File -ErrorAction SilentlyContinue)
        $jarCount = @($files | Where-Object { $_.Extension -eq '.jar' }).Count
        Say ('  [ok] ' + $path + '  (' + $files.Count + ' files, ' + $jarCount + ' jars)')
    } else {
        Say ('  [!!] not created: ' + $path)
    }
}
foreach ($group in @('com.mojang', 'net.minecraft')) {
    $dir = Join-Path (Join-Path $gradleHome 'caches\modules-2\files-2.1') $group
    if (Test-Path $dir) {
        $files = @(Get-ChildItem -Path $dir -Recurse -File -ErrorAction SilentlyContinue)
        Say ('  [ok] ' + $dir + '  (' + $files.Count + ' files)')
        foreach ($item in $files) {
            Say ('       ' + $item.FullName.Substring($dir.Length).TrimStart('\') + '  ' + $item.Length + ' B')
        }
    } else {
        Say ('  [!!] not cached: ' + $dir)
    }
}

# ---------------------------------------------------------------------------
Section 'Resolved compile classpath (gradle dshClasspath)'
$cpFile = Join-Path $ProjectDir 'build\dsh-cp.txt'
$gradlew = Join-Path $ProjectDir 'gradlew.bat'
if ($SkipGradle) {
    Say '  skipped (-SkipGradle)'
} elseif (Test-Path $gradlew) {
    Push-Location $ProjectDir
    try {
        $out = & $gradlew -q dshClasspath --no-problems-report 2>&1
        $out | ForEach-Object { Say ('  ' + $_) }
    } catch {
        Say ('  [!!] dshClasspath failed: ' + $_.Exception.Message)
    } finally {
        Pop-Location
    }
} else {
    Say '  [!!] gradlew.bat not found - cannot resolve the classpath'
}

$cpMc = @()
if (Test-Path $cpFile) {
    $all = @((Get-Content $cpFile -Raw) -split ';' | Where-Object { $_ -ne '' })
    Say ('  classpath entries          : ' + $all.Count)
    $cpMc = @($all | Where-Object { $_ -match '(?i)minecraft' })
    Say ('  entries matching minecraft : ' + $cpMc.Count)
    foreach ($entry in $cpMc) {
        Say ('    ' + $entry)
        $info = Get-JarInfo $entry
        if ($null -eq $info) {
            Say '      [!!] listed on the classpath but missing on disk'
        } else {
            Say ('      ' + (Mb $info.Size) + ', ' + $info.McEntries + ' net/minecraft entries')
            if ($info.Probe) { $script:Facts['CpHasMc'] = $true }
        }
    }
} else {
    Say '  [!!] build\dsh-cp.txt was not produced'
}

# ---------------------------------------------------------------------------
Section 'Verdict'
$mergedEntries = $script:Facts['MergedEntries']
$cpHasMc = $script:Facts.ContainsKey('CpHasMc') -and $script:Facts['CpHasMc']

if ($cpHasMc) {
    Say '  [OK] Minecraft classes are on the compile classpath.'
    Say '       If compileJava still fails, the errors are source-level. Full list:'
    Say '           build.bat compileJava -PdshMaxErrs=4000'
    Say '       Or let the checker decide: gradlew checkMinecraftClasspath'
} elseif ($cpMc.Count -eq 0) {
    Say '  [!!] Minecraft is NOT on the compile classpath (no minecraft jar resolved).'
    Say '       Look at the "Resolved compile classpath" section above and the'
    Say '       gradle -info log of the dependency resolution.'
} elseif ($null -eq $mergedJar -or $mergedEntries -le 0) {
    $clientEntries = $script:Facts['ClientEntries']
    Say '  [!!] The merge step is the broken link:'
    Say ('       caches\fabric-loom\' + $mcVersion + '\minecraft-merged.jar is ' +
         $(if ($null -eq $mergedJar) { 'missing' } else { (Mb $mergedJar.Size) + ' with ' + $mergedEntries + ' entries' }) + '.')
    if ($clientEntries -gt 1000) {
        Say ('       The downloads are fine (client: ' + (Mb $script:Facts['ClientSize']) + ', ' + $clientEntries + ' entries;' +
             ' server: ' + (Mb $script:Facts['ServerSize']) + ').')
        Say '       So the merge itself wrote nothing and Loom hid the reason: MinecraftJarMerger'
        Say '       submits its work to executors without keeping the Futures, so any exception'
        Say '       inside readToMap()/add() is discarded and the already-created output zip'
        Say '       stays empty (22 bytes, 0 entries).'
        Say ''
        Say '       Run the probe - it repeats the same zip/zipfs operations with the same JDK'
        Say '       and prints the exception Loom throws away:'
        Say '           build.bat probe'
    } else {
        Say '       client.jar itself looks empty or truncated, so the merge had nothing to work with.'
        Say '       Delete the files above and re-run:  build.bat repair'
    }
} elseif ($script:Facts['MavenSmallestSize'] -ge 0 -and $script:Facts['MavenSmallestSize'] -lt 1024) {
    Say '  [!!] The remap/jar-processing step is the broken link:'
    Say ('       minecraft-merged.jar is fine (' + (Mb $mergedJar.Size) + ', ' + $mergedEntries + ' entries)')
    Say ('       but the processed jar in minecraftMaven is only ' + $script:Facts['MavenSmallestSize'] + ' bytes' +
         ' (' + $script:Facts['MavenSmallestEntries'] + ' entries).')
    Say '       A 22 byte jar is an empty zip: the writer created it and added nothing.'
    Say '       Usual causes, in order of likelihood:'
    Say '         1) not enough disk space / antivirus deleting jar content while writing'
    Say '            -> free space above, Windows Security > Virus & threat protection >'
    Say '               Exclusions > add the project folder and GRADLE_USER_HOME'
    Say '         2) the Gradle JVM ran out of memory while remapping'
    Say '            -> raise org.gradle.jvmargs in gradle.properties (e.g. -Xmx6G)'
    Say '         3) a jar processor (mixin annotation processor / access widener) failed'
    Say '            silently -> build.bat build --info --stacktrace > build-log.txt'
    Say '       After changing anything: build.bat repair'
} else {
    Say '  [!!] A Minecraft jar is on the classpath but holds no net/minecraft classes,'
    Say '       while the merge looks fine. Attach build-diagnose.txt and the output of'
    Say '           build.bat build --info --stacktrace > build-log.txt'
}

try {
    Set-Content -Path $ReportPath -Value $script:Lines -Encoding UTF8
    Say ''
    Say ('Report written to ' + $ReportPath)
} catch {
    Write-Host ('Could not write ' + $ReportPath + ': ' + $_.Exception.Message)
}

exit 0
