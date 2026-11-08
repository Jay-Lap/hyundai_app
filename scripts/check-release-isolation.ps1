# Searches known demo-only payloads in every uncompressed APK entry, in UTF-8 and UTF-16LE.
param(
    [string]$DemoApk,
    [string]$ReleaseApk
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$workspace = Split-Path -Parent $PSScriptRoot
if (-not $DemoApk) { $DemoApk = Join-Path $workspace 'app/build/outputs/apk/demo/app-demo.apk' }
if (-not $ReleaseApk) { $ReleaseApk = Join-Path $workspace 'app/build/outputs/apk/release/app-release-unsigned.apk' }
$encoding = [Text.Encoding]::GetEncoding('ISO-8859-1')
# UTF-8 bytes avoid Windows PowerShell 5.1 interpreting a UTF-8 script as ANSI.
$markers = @(
    @{ Name = 'demo-route'; Hex = '44 45 4D 4F 2D 52 4F 55 54 45 2D 41' },
    @{ Name = 'demo-route-b'; Hex = '44 45 4D 4F 2D 52 4F 55 54 45 2D 42' },
    @{ Name = 'diagnostic-screen'; Hex = 'EA B0 9C EB B0 9C EC 9A A9 20 EC A7 84 EB 8B A8' },
    @{ Name = 'navigation-diagnostics'; Hex = 'EB 82 B4 EB B9 84 EA B2 8C EC 9D B4 EC 85 98 20 EC A7 84 EB 8B A8 20 28 ED 99 94 EB A9 B4 EC 9D 84 20 EB 82 98 EA B0 80 EB A9 B4 20 EC 82 AD EC A0 9C 29' },
    @{ Name = 'option-callback-trace'; Hex = 'EC 98 B5 EC 85 98 20 EB B3 80 EA B2 BD 20 EC BD 9C EB B0 B1' }
)

function Read-ApkContent([string]$path) {
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $entries = @($archive.Entries | Where-Object { $_.FullName -match '^classes\d*\.dex$' })
        if ($entries.Count -eq 0) { throw "No DEX files: $path" }
        $result = [Text.StringBuilder]::new()
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.EndsWith('/')) { continue }
            $reader = [IO.StreamReader]::new($entry.Open(), $encoding, $false)
            try { [void]$result.Append($reader.ReadToEnd()) } finally { $reader.Dispose() }
        }
        return $result.ToString()
    } finally { $archive.Dispose() }
}

$demo = Read-ApkContent $DemoApk
$release = Read-ApkContent $ReleaseApk
foreach ($marker in $markers) {
    $bytes = [byte[]]($marker.Hex -split ' ' | ForEach-Object { [Convert]::ToByte($_, 16) })
    $utf8 = $encoding.GetString($bytes)
    $utf16 = $encoding.GetString([Text.Encoding]::Unicode.GetBytes([Text.Encoding]::UTF8.GetString($bytes)))
    if (-not ($demo.Contains($utf8) -or $demo.Contains($utf16))) { throw "Expected demo marker not found: $($marker.Name)" }
    if ($release.Contains($utf8) -or $release.Contains($utf16)) { throw "Demo marker found in release: $($marker.Name)" }
    Write-Output "OK: demo contains marker; release does not: $($marker.Name)"
}
Write-Output 'Known markers checked in APK entries; obfuscated/encoded payloads and runtime isolation remain unverified.'
