# -----------------------------
# PowerShell build script for Biotak indicator
# Usage:
#   .\build.ps1          -> Compile & package jar (normal deploy)
#   .\build.ps1 dev      -> Compile classes into Extensions/dev for hot-reload
# -----------------------------

param(
    [string]$Mode = "jar"
)

# Set the paths (edit if your paths differ)
$JAVA_HOME = "C:\Program Files\Java\jdk-23"
$MWAVE_SDK_JAR = "lib\mwave_sdk.jar"
$SRC_DIR = "src"
$BUILD_DIR = "build\classes"
$EXT_DIR = "C:\Users\fatemeh\MotiveWave Extensions"

Write-Host "Starting Biotak build process..."

# Clean previous build
if (Test-Path $BUILD_DIR) {
    Remove-Item -Recurse -Force $BUILD_DIR
}
New-Item -ItemType Directory -Force -Path $BUILD_DIR | Out-Null

# Find all Java files
$javaFiles = Get-ChildItem -Path $SRC_DIR -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
$javaFilesList = $javaFiles -join " "

Write-Host "Found $($javaFiles.Count) Java files to compile..."

# Compile sources
Write-Host "Compiling Java files..."

try {
    & "$JAVA_HOME\bin\javac" -cp $MWAVE_SDK_JAR -d $BUILD_DIR -nowarn $javaFiles
    if ($LASTEXITCODE -ne 0) {
        throw "Compilation failed with exit code $LASTEXITCODE"
    }
    Write-Host "Compilation successful." -ForegroundColor Green
} catch {
    Write-Error "Compilation failed: $_"
    exit 1
}

if ($Mode -eq "dev") {
    Write-Host "Deploying class files to $EXT_DIR\dev ..."
    
    $devDir = "$EXT_DIR\dev"
    if (Test-Path $devDir) {
        Remove-Item -Recurse -Force $devDir
    }
    New-Item -ItemType Directory -Force -Path $devDir | Out-Null
    
    # Copy all compiled classes
    Copy-Item -Path "$BUILD_DIR\*" -Destination $devDir -Recurse -Force
    
    # Touch last updated file
    New-Item -ItemType File -Force -Path "$EXT_DIR\.last_updated" | Out-Null
    
    Write-Host "Build successful - dev classes copied." -ForegroundColor Green
    exit 0
}

# Otherwise build jar
Write-Host "Creating JAR file..."
$JAR_OUT = "build\biotak.jar"
New-Item -ItemType Directory -Force -Path "build" | Out-Null

Push-Location $BUILD_DIR
try {
    & "$JAVA_HOME\bin\jar" cf "..\biotak.jar" com
    if ($LASTEXITCODE -ne 0) {
        throw "JAR creation failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Error "JAR creation failed: $_"
    Pop-Location
    exit 1
}
Pop-Location

# Deploy jar to MotiveWave Extensions
$libDir = "$EXT_DIR\lib"
New-Item -ItemType Directory -Force -Path $libDir | Out-Null
Copy-Item -Path $JAR_OUT -Destination $libDir -Force

# Touch last updated file
New-Item -ItemType File -Force -Path "$EXT_DIR\.last_updated" | Out-Null

Write-Host "Build successful - jar deployed to MotiveWave Extensions\lib." -ForegroundColor Green
