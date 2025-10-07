@echo off
REM ======================================================
REM  Create Symbolic Link for MT4 Biotak Indicator
REM ======================================================
REM This script must run as Administrator

echo.
echo ========================================
echo   MT4 Symbolic Link Creator
echo ========================================
echo.

REM Check for admin privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] This script requires Administrator privileges!
    echo.
    echo Please right-click on this file and select "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo [INFO] Running with Administrator privileges...
echo.

REM Define paths
set "SOURCE=C:\Users\Fatemehkh\IdeaProject\Biotak\Mt4\Biotak Trigger TH3"
set "TARGET=C:\Users\Fatemehkh\AppData\Roaming\MetaQuotes\Terminal\0727F3F88B5F0FE006962B330B91FF37\MQL4\Indicators\Biotak Trigger TH3"

echo Source: %SOURCE%
echo Target: %TARGET%
echo.

REM Check if source exists
if not exist "%SOURCE%" (
    echo [ERROR] Source folder not found!
    echo Path: %SOURCE%
    pause
    exit /b 1
)

REM Remove existing target if it exists
if exist "%TARGET%" (
    echo [INFO] Removing existing target...
    rmdir /S /Q "%TARGET%" 2>nul
    if exist "%TARGET%" (
        echo [WARNING] Could not remove existing target. Please close MT4 and try again.
        pause
        exit /b 1
    )
)

REM Create symbolic link
echo [INFO] Creating symbolic link...
mklink /D "%TARGET%" "%SOURCE%"

if %errorLevel% equ 0 (
    echo.
    echo ========================================
    echo   SUCCESS!
    echo ========================================
    echo.
    echo Symbolic link created successfully!
    echo.
    echo Now any changes you make in your project will be
    echo immediately visible in MT4 - no copying needed!
    echo.
    echo Test it:
    echo 1. Open your project files in an editor
    echo 2. Make a small change and save
    echo 3. Open MT4 MetaEditor - you'll see the change instantly!
    echo.
) else (
    echo.
    echo [ERROR] Failed to create symbolic link!
    echo Error code: %errorLevel%
    echo.
)

pause
