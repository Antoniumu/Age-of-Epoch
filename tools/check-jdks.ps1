param([Parameter(Mandatory=$true)][string]$ProjectDir)
$ErrorActionPreference = 'Stop'
$homes = @($env:JAVA_HOME)
$patterns = @(
    "$ProjectDir\.jdks\*", "$env:USERPROFILE\.jdks\*",
    "$env:ProgramFiles\Eclipse Adoptium\jdk-*", "$env:ProgramFiles\Java\jdk-*",
    "$env:ProgramFiles\Microsoft\jdk-*", "$env:ProgramFiles\Zulu\zulu-*",
    "$env:ProgramFiles\Amazon Corretto\jdk*", "$env:ProgramFiles\BellSoft\LibericaJDK-*",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-*",
    "$env:LOCALAPPDATA\Programs\Microsoft\jdk-*"
)
foreach ($pattern in $patterns) {
    $homes += @(Get-Item $pattern -ErrorAction SilentlyContinue | Where-Object PSIsContainer | ForEach-Object FullName)
}
$homes += @(Get-Command java.exe -All -ErrorAction SilentlyContinue | ForEach-Object {
    Split-Path (Split-Path $_.Source -Parent) -Parent
})
$seen = @{}
$results = @()
foreach ($homeDir in $homes) {
    if (-not $homeDir -or -not (Test-Path "$homeDir\bin\javac.exe")) { continue }
    $homeDir = (Resolve-Path -LiteralPath $homeDir).ProviderPath.TrimEnd('\')
    if ($seen.ContainsKey($homeDir)) { continue }
    $seen[$homeDir] = $true
    $major = 0
    if (Test-Path "$homeDir\release") {
        $version = Get-Content "$homeDir\release" | Where-Object { $_ -match '^JAVA_VERSION=' } | Select-Object -First 1
        if ($version -match '="(\d+)') { $major = [int]$Matches[1] }
    }
    if ($major -lt 11) { continue }
    Write-Host "`n--- $homeDir"
    & "$homeDir\bin\java.exe" "$ProjectDir\tools\WritableCheck.java" $ProjectDir $env:TEMP $env:GRADLE_USER_HOME
    $code = $LASTEXITCODE
    $verdict = switch ($code) { 0 {'OK'} 2 {'DENIED'} 3 {'MISREPORT'} default {'CHECK FAILED'} }
    $eligible = if ($code -eq 0 -and $major -ge 25) { 'YES' } else { 'NO' }
    $results += [pscustomobject]@{JDK=$homeDir; Major=$major; Verdict=$verdict; 'Build eligible'=$eligible}
}
if ($results.Count -eq 0) { Write-Host 'No JDK 11+ found.'; exit 1 }
Write-Host "`nSummary: build requires Java 25+ AND a successful writability check."
$results | Format-Table -AutoSize -Wrap | Out-Host
if (@($results | Where-Object { $_.'Build eligible' -eq 'YES' }).Count -eq 0) {
    Write-Host 'No build-ready JDK found. Install another JDK 25, then run jcheck again.'
}
exit 0
