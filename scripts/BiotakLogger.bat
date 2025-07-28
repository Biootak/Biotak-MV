@echo off
:: ============================================================================
:: Biotak Batch Script Logging Integration
:: سیستم لاگینگ یکپارچه برای Batch Scripts
:: ============================================================================

:: Configuration
set LOG_DIR=C:\Users\fatemeh\IdeaProject\Biotak\logs
set LOG_FILE=%LOG_DIR%\biotak_batch.log
set ERROR_LOG_FILE=%LOG_DIR%\biotak_errors.log
set MAX_FILE_SIZE=10485760
set TIMESTAMP_FORMAT=yyyy-MM-dd HH:mm:ss.SSS

:: Ensure log directory exists
if not exist "%LOG_DIR%" (
    md "%LOG_DIR%"
)

:: Function to rotate log file if needed
:rotateLogFile
setlocal
set FILE_SIZE=0
for %%F in (%LOG_FILE%) do set FILE_SIZE=%%~zF
if %FILE_SIZE% GTR %MAX_FILE_SIZE% (
    set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
    set ROTATED_NAME=%LOG_FILE%.%TIMESTAMP%
    rename "%LOG_FILE%" "%ROTATED_NAME%"
    echo Rotated log file: %LOG_FILE% -> %ROTATED_NAME%

    :: Clean up old backups (keep last 5)
    set /a COUNT=0
    for /f "tokens=* delims=" %%F in ('dir "%LOG_FILE%.*" /b /A-D /O-D') do (
        set /a COUNT+=1
        if !COUNT! GTR 5 del "%%F"
    )
)
endlocal
exit /b

:: Function to format and log a message
:logMessage
setlocal
set LEVEL=%~1
set MESSAGE=%~2
set TIMESTAMP=%date:~-4,4%-%date:~-10,2%-%date:~-7,2% %time:~0,2%:%time:~3,2%:%time:~6,2%:%time:~9,2%
set LOG_ENTRY=%TIMESTAMP% [%LEVEL%] [BAT] %~nx0 - %MESSAGE%

:: Write to console
if "%LEVEL%"=="ERROR" (
    echo %LOG_ENTRY%
) else (
    echo %LOG_ENTRY%
)

:: Rotate log file if needed
call :rotateLogFile

:: Write to main log file
>>"%LOG_FILE%" echo %LOG_ENTRY%

:: Write errors to error log as well
if "%LEVEL%"=="ERROR" (
    >>"%ERROR_LOG_FILE%" echo %LOG_ENTRY%
)

endlocal
exit /b

:: ============================================================================

:logTrace
call :logMessage "TRACE" "%~1"
exit /b

:logDebug
call :logMessage "DEBUG" "%~1"
exit /b

:logInfo
call :logMessage "INFO" "%~1"
exit /b

:logWarn
call :logMessage "WARN" "%~1"
exit /b

:logError
call :logMessage "ERROR" "%~1"
exit /b

:main
:: Example usage
call :logInfo "Starting batch script..."
:: Perform your batch operations here
call :logError "An error occurred"
exit /b

:main

