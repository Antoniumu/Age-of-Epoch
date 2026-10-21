param([Parameter(Mandatory=$true)][string]$ProjectDir)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
try {
    $uri = [uri]$env:AOE_JDK_URL
    if (-not $uri.IsAbsoluteUri -or $uri.Scheme -ne 'https') { throw 'AOE_JDK_URL must be a trusted HTTPS JDK ZIP URL.' }
    $destination = Join-Path $ProjectDir '.jdks'
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    $zip = Join-Path $destination ([guid]::NewGuid().ToString() + '.zip')
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $zip
        Expand-Archive -LiteralPath $zip -DestinationPath $destination -Force
    } finally {
        if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }
    }
} catch {
    Write-Error $_
    exit 1
}
