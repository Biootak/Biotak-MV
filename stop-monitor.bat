@echo off
echo 🛑 Stopping Biotak Real-Time Log Monitor...
echo.

REM Kill java processes related to monitor
taskkill /F /IM java.exe 2>nul
if %errorlevel%==0 (
    echo ✅ Monitor stopped successfully!
) else (
    echo ℹ️  No running monitor found.
)

echo.
echo 🧹 Cleaning up temporary files...
if exist logs\*.tmp del logs\*.tmp

echo.
echo ✅ Cleanup completed!
echo 🚀 You can now restart the monitor safely.
echo.
pause
