# =============================================================================
# Comprehensive PowerShell build script for Biotak Financial Indicator
# =============================================================================
# Usage:
#   .\build.ps1           -> Compile & package jar (default)
#   .\build.ps1 dev       -> Hot-reload development mode
#   .\build.ps1 test      -> Run tests after compilation
#   .\build.ps1 clean     -> Clean build and rebuild
#   .\build.ps1 release   -> Build optimized release version
# =============================================================================

param(
    [string]$Mode = "jar"
)

# Function to print colored output
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Blue }
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Warning { param($Message) Write-Host "[WARNING] $Message" -ForegroundColor Yellow }
function Write-ErrorMsg { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# Function to handle errors and exit
function Exit-WithError {
    param($Message)
    Write-ErrorMsg $Message
    exit 1
}

# Validate environment
function Test-Environment {
    Write-Info "Validating build environment..."
    
    # Check Java installation
    if (-Not (Test-Path "$JAVA_HOME\bin\javac.exe")) {
        Exit-WithError "javac not found at $JAVA_HOME\bin\javac.exe"
    }
    
    # Check SDK dependency
    if (-Not (Test-Path $MWAVE_SDK_JAR)) {
        Exit-WithError "MotiveWave SDK not found at $MWAVE_SDK_JAR"
    }
    
    Write-Success "Environment validation passed"
}

# Display build information
function Show-BuildInfo {
    Write-Host "====================================="
    Write-Host "    BIOTAK BUILD INFORMATION"
    Write-Host "====================================="
    $javaVersion = & "$JAVA_HOME\bin\java" -version 2>&1 | Select-Object -First 1
    Write-Host "Java Version: $javaVersion"
    Write-Host "Build Mode: $Mode"
    Write-Host "Source Directory: $SRC_DIR"
    Write-Host "Build Directory: $BUILD_DIR"
    Write-Host "Target Directory: $EXT_DIR"
    Write-Host "====================================="
}

# Configuration - Force Java 24 to match MotiveWave SDK version requirements
$JAVA_HOME = "C:\Program Files\Java\jdk-24"
$MWAVE_SDK_JAR = "lib\mwave_sdk.jar"
$JUNIT_JAR = "lib\junit-platform-console-standalone-1.10.0.jar"
$SRC_DIR = "src"
$BUILD_DIR = "build\classes"
$TEST_DIR = "build\test-classes"
$EXT_DIR = "C:\Users\Fatemehkh\MotiveWave Extensions"
$JAR_OUT = "build\biotak.jar"

# Validate environment first
Test-Environment
Show-BuildInfo

# Handle clean mode
if ($Mode -eq "clean") {
    Write-Info "Cleaning build directories..."
    if (Test-Path "build") { Remove-Item -Recurse -Force "build" }
    if (Test-Path "out") { Remove-Item -Recurse -Force "out" }
    Write-Success "Clean completed"
    exit 0
}

# Create build directories
Write-Info "Setting up build directories..."
New-Item -ItemType Directory -Force -Path $BUILD_DIR | Out-Null
New-Item -ItemType Directory -Force -Path $TEST_DIR | Out-Null
New-Item -ItemType Directory -Force -Path "build" | Out-Null

# Find all Java files
$javaFiles = Get-ChildItem -Path $SRC_DIR -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
Write-Info "Found $($javaFiles.Count) Java source files"

# Compile main sources
Write-Info "Compiling main sources..."

try {
    & "$JAVA_HOME\bin\javac" -cp $MWAVE_SDK_JAR -d $BUILD_DIR -Xlint:deprecation -Xlint:unchecked $javaFiles
    if ($LASTEXITCODE -ne 0) {
        throw "Compilation failed with exit code $LASTEXITCODE"
    }
    Write-Success "Main compilation completed successfully"
} catch {
    Exit-WithError "Main compilation failed: $_"
}

# Handle test mode
if ($Mode -eq "test") {
    Write-Info "Compiling and running tests..."
    
    # Find test files
    $testFiles = Get-ChildItem -Path $SRC_DIR -Filter "*Test*.java" -Recurse
    $testFiles += Get-ChildItem -Path $SRC_DIR -Filter "*test*.java" -Recurse
    
    if ($testFiles.Count -gt 0) {
        Write-Info "Compiling test files..."
        $testFilePaths = $testFiles | ForEach-Object { $_.FullName }
        
        try {
            & "$JAVA_HOME\bin\javac" -cp "$MWAVE_SDK_JAR;$JUNIT_JAR;$BUILD_DIR" -d $TEST_DIR $testFilePaths
            if ($LASTEXITCODE -ne 0) {
                throw "Test compilation failed with exit code $LASTEXITCODE"
            }
            
            Write-Info "Running tests..."
            & "$JAVA_HOME\bin\java" -cp "$JUNIT_JAR;$BUILD_DIR;$TEST_DIR" org.junit.platform.console.ConsoleLauncher --scan-classpath
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Some tests failed"
            }
        } catch {
            Exit-WithError "Test compilation failed: $_"
        }
    } else {
        Write-Warning "No test files found"
    }
    exit 0
}

# Handle dev mode
if ($Mode -eq "dev") {
    Write-Info "Deploying classes for development mode..."
    
    $devDir = "$EXT_DIR\dev"
    if (Test-Path $devDir) {
        Remove-Item -Recurse -Force $devDir
    }
    New-Item -ItemType Directory -Force -Path $devDir | Out-Null
    
    # Copy all compiled classes
    Copy-Item -Path "$BUILD_DIR\*" -Destination $devDir -Recurse -Force
    
    # Touch last updated file
    New-Item -ItemType File -Force -Path "$EXT_DIR\.last_updated" | Out-Null
    
    Write-Success "Development classes deployed to $devDir"
    Write-Info "MotiveWave will hot-reload these classes automatically"
    exit 0
}

# Build JAR (default and release modes)
Write-Info "Creating JAR package..."

# Create manifest
$manifestFile = "build\MANIFEST.MF"
$manifestContent = @"
Manifest-Version: 1.0
Created-By: Biotak Build System
Implementation-Title: Biotak Financial Indicator
Implementation-Version: 1.0.0
Implementation-Vendor: Biotak Development Team
Build-Date: $(Get-Date)
Main-Class: com.biotak.BiotakTrigger

"@

$manifestContent | Out-File -FilePath $manifestFile -Encoding ASCII

# Create JAR with manifest
Push-Location $BUILD_DIR
try {
    & "$JAVA_HOME\bin\jar" cfm "..\biotak.jar" "..\MANIFEST.MF" com
    if ($LASTEXITCODE -ne 0) {
        throw "JAR creation failed with exit code $LASTEXITCODE"
    }
} catch {
    Pop-Location
    Exit-WithError "JAR creation failed: $_"
}
Pop-Location

Write-Success "JAR created: $JAR_OUT"

# Deploy to MotiveWave
Write-Info "Deploying to MotiveWave Extensions..."
$libDir = "$EXT_DIR\lib"
New-Item -ItemType Directory -Force -Path $libDir | Out-Null
Copy-Item -Path $JAR_OUT -Destination $libDir -Force

# Update timestamp
New-Item -ItemType File -Force -Path "$EXT_DIR\.last_updated" | Out-Null

# Show deployment info
$jarSize = (Get-Item $JAR_OUT).Length
$jarSizeKB = [math]::Round($jarSize / 1024, 2)
Write-Success "Build completed successfully!"
Write-Info "JAR size: $jarSizeKB KB"
Write-Info "Deployed to: $libDir\biotak.jar"
Write-Info "MotiveWave Extensions directory updated"

if ($Mode -eq "release") {
    Write-Info "Release mode: Consider running tests with '.\build.ps1 test'"
}
