# ==============================================================================
# Biotak PowerShell Logging Integration
# سیستم لاگینگ یکپارچه برای PowerShell Scripts
# ==============================================================================

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL")]
    [string]$Level = "INFO",
    
    [Parameter(Mandatory=$true)]
    [string]$Message,
    
    [Parameter(Mandatory=$false)]
    [string]$ScriptName = $MyInvocation.ScriptName,
    
    [Parameter(Mandatory=$false)]
    [string]$LogDir = "C:\Users\fatemeh\IdeaProject\Biotak\logs"
)

# ==============================================================================
# CONFIGURATION
# ==============================================================================

$LOG_FILE = Join-Path $LogDir "biotak_powershell.log"
$ERROR_LOG_FILE = Join-Path $LogDir "biotak_errors.log"
$MAX_FILE_SIZE = 10MB
$TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.fff"

# Color mapping for console output
$Colors = @{
    "TRACE" = "White"
    "DEBUG" = "Cyan"
    "INFO"  = "Green"
    "WARN"  = "Yellow"
    "ERROR" = "Red"
    "FATAL" = "Magenta"
}

# ==============================================================================
# FUNCTIONS
# ==============================================================================

function Ensure-LogDirectory {
    if (-not (Test-Path $LogDir)) {
        try {
            New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
            Write-Host "Created log directory: $LogDir" -ForegroundColor Green
        }
        catch {
            Write-Error "Failed to create log directory: $LogDir - $_"
            return $false
        }
    }
    return $true
}

function Rotate-LogFile {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        $file = Get-Item $FilePath
        if ($file.Length -gt $MAX_FILE_SIZE) {
            $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
            $rotatedName = "$FilePath.$timestamp"
            
            try {
                Move-Item $FilePath $rotatedName
                Write-Host "Rotated log file: $FilePath -> $rotatedName" -ForegroundColor Yellow
                
                # Clean up old backup files (keep last 5)
                $backups = Get-ChildItem -Path (Split-Path $FilePath) -Filter "$(Split-Path $FilePath -Leaf).*" | 
                          Where-Object { $_.Name -match "\.\d{8}_\d{6}$" } |
                          Sort-Object LastWriteTime -Descending
                
                if ($backups.Count -gt 5) {
                    $backups | Select-Object -Skip 5 | Remove-Item -Force
                }
            }
            catch {
                Write-Error "Failed to rotate log file: $_"
            }
        }
    }
}

function Format-LogMessage {
    param(
        [string]$Level,
        [string]$Message,
        [string]$ScriptName
    )
    
    $timestamp = Get-Date -Format $TIMESTAMP_FORMAT
    $threadId = [System.Threading.Thread]::CurrentThread.ManagedThreadId
    $scriptNameOnly = if ($ScriptName) { Split-Path $ScriptName -Leaf } else { "PowerShell" }
    
    return "$timestamp [$Level] [PS] [$threadId] PowerShell.$scriptNameOnly() - $Message"
}

function Write-LogMessage {
    param(
        [string]$Level,
        [string]$Message,
        [string]$ScriptName
    )
    
    # Ensure log directory exists
    if (-not (Ensure-LogDirectory)) {
        return
    }
    
    # Format the message
    $formattedMessage = Format-LogMessage -Level $Level -Message $Message -ScriptName $ScriptName
    
    # Write to console with color
    $color = $Colors[$Level]
    if ($color) {
        Write-Host $formattedMessage -ForegroundColor $color
    } else {
        Write-Host $formattedMessage
    }
    
    # Rotate log file if needed
    Rotate-LogFile -FilePath $LOG_FILE
    
    # Write to main log file
    try {
        Add-Content -Path $LOG_FILE -Value $formattedMessage -Encoding UTF8
    }
    catch {
        Write-Error "Failed to write to log file: $_"
    }
    
    # Write errors to error log as well
    if ($Level -eq "ERROR" -or $Level -eq "FATAL") {
        Rotate-LogFile -FilePath $ERROR_LOG_FILE
        try {
            Add-Content -Path $ERROR_LOG_FILE -Value $formattedMessage -Encoding UTF8
        }
        catch {
            Write-Error "Failed to write to error log file: $_"
        }
    }
}

# ==============================================================================
# CONVENIENCE FUNCTIONS
# ==============================================================================

function Write-BiotakTrace {
    param([string]$Message, [string]$ScriptName = $MyInvocation.ScriptName)
    Write-LogMessage -Level "TRACE" -Message $Message -ScriptName $ScriptName
}

function Write-BiotakDebug {
    param([string]$Message, [string]$ScriptName = $MyInvocation.ScriptName)
    Write-LogMessage -Level "DEBUG" -Message $Message -ScriptName $ScriptName
}

function Write-BiotakInfo {
    param([string]$Message, [string]$ScriptName = $MyInvocation.ScriptName)
    Write-LogMessage -Level "INFO" -Message $Message -ScriptName $ScriptName
}

function Write-BiotakWarn {
    param([string]$Message, [string]$ScriptName = $MyInvocation.ScriptName)
    Write-LogMessage -Level "WARN" -Message $Message -ScriptName $ScriptName
}

function Write-BiotakError {
    param([string]$Message, [string]$ScriptName = $MyInvocation.ScriptName)
    Write-LogMessage -Level "ERROR" -Message $Message -ScriptName $ScriptName
}

function Write-BiotakFatal {
    param([string]$Message, [string]$ScriptName = $MyInvocation.ScriptName)
    Write-LogMessage -Level "FATAL" -Message $Message -ScriptName $ScriptName
}

# ==============================================================================
# PERFORMANCE TRACKING
# ==============================================================================

$Global:BiotakPerformanceTrackers = @{}

function Start-BiotakPerformanceTracking {
    param([string]$Operation)
    
    $Global:BiotakPerformanceTrackers[$Operation] = Get-Date
    Write-BiotakInfo "Started tracking: $Operation"
}

function Stop-BiotakPerformanceTracking {
    param([string]$Operation)
    
    if ($Global:BiotakPerformanceTrackers.ContainsKey($Operation)) {
        $startTime = $Global:BiotakPerformanceTrackers[$Operation]
        $duration = (Get-Date) - $startTime
        $durationMs = [math]::Round($duration.TotalMilliseconds)
        
        Write-BiotakInfo "Operation '$Operation' took $durationMs ms"
        
        # Warn for slow operations
        if ($durationMs -gt 1000) {
            Write-BiotakWarn "SLOW OPERATION: '$Operation' took $durationMs ms"
        }
        
        $Global:BiotakPerformanceTrackers.Remove($Operation)
    }
}

# ==============================================================================
# MAIN EXECUTION
# ==============================================================================

# If called directly with parameters, log the message
if ($Message) {
    Write-LogMessage -Level $Level -Message $Message -ScriptName $ScriptName
}

# ==============================================================================
# EXPORTS
# ==============================================================================

# Export functions for use in other scripts
Export-ModuleMember -Function @(
    'Write-BiotakTrace',
    'Write-BiotakDebug', 
    'Write-BiotakInfo',
    'Write-BiotakWarn',
    'Write-BiotakError',
    'Write-BiotakFatal',
    'Start-BiotakPerformanceTracking',
    'Stop-BiotakPerformanceTracking'
)

# ==============================================================================
# USAGE EXAMPLES
# ==============================================================================

<#
# استفاده مستقیم از فایل
.\BiotakLogger.ps1 -Level "INFO" -Message "Application started successfully"

# استفاده در اسکریپت دیگر
. .\BiotakLogger.ps1
Write-BiotakInfo "This is an info message"
Write-BiotakError "This is an error message"

# Performance tracking
Start-BiotakPerformanceTracking "DataProcessing"
# ... some work ...
Stop-BiotakPerformanceTracking "DataProcessing"
#>
