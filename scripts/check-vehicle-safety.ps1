# Scoped static check, not a proof against reflection, dynamic code, or SDK internals.
param([Parameter(Mandatory = $true)][string]$BuildRoot)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $root 'app/src'
$allowedReaders = @('VehicleDoorReader.kt', 'VehicleHealthReader.kt', 'WiperReader.kt')
$productionSets = @('main', 'debug', 'demo', 'release')
$constructorFiles = @()
foreach ($set in $productionSets) {
    $setRoot = Join-Path $sourceRoot $set
    if (-not (Test-Path -LiteralPath $setRoot)) { continue }
    foreach ($file in Get-ChildItem -LiteralPath $setRoot -Recurse -File | Where-Object { $_.Extension -in '.kt', '.java' }) {
        $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
        if ($content -match 'ai\.pleos\.playground\.vehicle' -and ($set -ne 'main' -or $file.Name -notin $allowedReaders)) {
            throw "Unexpected Vehicle SDK reference: $($file.FullName)"
        }
        foreach ($line in ($content -split "`n")) {
            if ($line -match '\bVehicle\s*\(') {
                if ($set -ne 'main' -or $file.Name -notin $allowedReaders -or $line -notmatch 'VehicleSdkAccess\.create\s*\{\s*Vehicle\s*\(') {
                    throw "Unguarded Vehicle construction: $($file.FullName)"
                }
                $constructorFiles += $file.Name
            }
        }
    }
}
if ($constructorFiles.Count -ne 3 -or (@($constructorFiles | Sort-Object -Unique).Count -ne 3)) { throw 'Expected exactly one guarded constructor in each reader.' }
$policy = Get-Content -LiteralPath (Join-Path $sourceRoot 'main/java/com/example/hyundiaboysranger/VehicleSdkAccess.kt') -Raw -Encoding UTF8
if ($policy -notmatch 'const\s+val\s+enabled\s*=\s*false\b' -or $policy -notmatch 'if\s*\(enabled\)\s*factory\(\)\s*else\s*null') { throw 'Vehicle safety policy changed.' }
$javap = Join-Path $env:JAVA_HOME 'bin/javap.exe'
if (-not (Test-Path -LiteralPath $javap)) { throw 'JAVA_HOME must point to a JDK.' }
foreach ($variant in @('debug', 'demo', 'release')) {
    # Gradle capitalizes task names (compileDebugKotlin, etc.).
    $taskName = 'compile' + (Get-Culture).TextInfo.ToTitleCase($variant) + 'Kotlin'
    $classRoot = Join-Path $BuildRoot "app/intermediates/built_in_kotlinc/$variant/$taskName/classes"
    $package = Join-Path $classRoot 'com/example/hyundiaboysranger'
    if (-not (Test-Path -LiteralPath $package)) { throw "Missing compiled classes for $variant" }
    $classes = Get-ChildItem -LiteralPath $package -File | Where-Object { $_.Name -match '^(VehicleDoorReader|VehicleHealthReader|WiperReader)(\$.*)?\.class$' }
    if ($classes.Count -lt 3) { throw "Missing reader classes for $variant" }
    $names = @($classes | ForEach-Object { 'com.example.hyundiaboysranger.' + $_.BaseName })
    $output = & $javap -p -c -classpath $classRoot $names 2>&1
    if ($LASTEXITCODE -ne 0) { throw "javap failed for $variant" }
    if (($output | Out-String) -match 'new\s+#\d+\s+// class ai/pleos/playground/vehicle/Vehicle\b') { throw "Vehicle constructor bytecode found in $variant reader classes" }
    Write-Output "OK: $variant reader bytecode has no Vehicle allocation; three source constructors are gated."
}
Write-Output 'Static checks cover named source sets and reader classes; reflection, native code, dependency internals and runtime behavior remain outside scope.'
