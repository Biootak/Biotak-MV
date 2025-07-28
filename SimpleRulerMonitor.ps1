# ==============================================================================
# BIOTAK RULER LOG MONITOR - Real-time ruler log monitoring
# ==============================================================================

param(
    [Parameter(Mandatory=$false)]
    [int]$RefreshInterval = 1,
    
    [Parameter(Mandatory=$false)]
    [switch]$ShowAll = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$FilterRuler = $true
)

# Console colors
$Colors = @{
    "Header" = "Cyan"
    "Success" = "Green" 
    "Warning" = "Yellow"
    "Error" = "Red"
    "Info" = "White"
    "Debug" = "Gray"
    "Ruler" = "Magenta"
    "Performance" = "Blue"
    "Timestamp" = "DarkGray"
}

# Log file paths to monitor
$LogFiles = @(
    "biotak_log.txt",
    "performance_monitor.log",
    "logs\biotak_powershell.log",
    "logs\biotak_errors.log"
)

# Ruler-related keywords
$RulerKeywords = @(
    "ruler", "[rul]", "onclick", "onmousemove", "onmousedown", 
    "resize", "distance", "pips", "bars", "coordinate", 
    "translate", "rulerstate", "waiting_for_start", 
    "waiting_for_end", "active", "inactive"
)

# Global variables
$Global:TotalLogs = 0
$Global:RulerEvents = 0
$Global:LastPositions = @{}

function Show-Header {
    Clear-Host
    Write-Host "================================================================================" -ForegroundColor $Colors.Header
    Write-Host "                    BIOTAK RULER LOG MONITOR                                    " -ForegroundColor $Colors.Header  
    Write-Host "================================================================================" -ForegroundColor $Colors.Header
    Write-Host ""
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "Started at: $timestamp" -ForegroundColor $Colors.Info
    Write-Host "Focus: $(if($FilterRuler) {'Ruler Events Only'} else {'All Log Events'})" -ForegroundColor $Colors.Info
    Write-Host "Refresh Rate: $RefreshInterval second(s)" -ForegroundColor $Colors.Info
    Write-Host ""
    
    Write-Host "MONITORING FILES:" -ForegroundColor $Colors.Warning
    foreach ($file in $LogFiles) {
        $exists = Test-Path $file
        $status = if ($exists) { "[OK]" } else { "[--]" }
        $color = if ($exists) { $Colors.Success } else { $Colors.Error }
        Write-Host "  $status $file" -ForegroundColor $color
    }
    Write-Host ""
    Write-Host "--------------------------------------------------------------------------------" -ForegroundColor $Colors.Header
    Write-Host "LIVE LOG STREAM (Press Ctrl+C to stop):" -ForegroundColor $Colors.Info
    Write-Host ""
}

function Test-RulerRelated {
    param([string]$LogLine)
    
    if (-not $FilterRuler) { return $true }
    
    $lowerLine = $LogLine.ToLower()
    foreach ($keyword in $RulerKeywords) {
        if ($lowerLine.Contains($keyword.ToLower())) {
            return $true
        }
    }
    
    # Also include critical errors
    return ($lowerLine.Contains("[error]") -or $lowerLine.Contains("[fatal]"))
}

function Format-LogEntry {
    param([string]$LogLine, [string]$SourceFile)
    
    $timestamp = Get-Date -Format "HH:mm:ss.fff"
    $prefix = "[$timestamp]"
    
    # Determine log type and color
    $color = $Colors.Info
    $icon = "LOG"
    
    if ($LogLine -match "\[ERROR\]|\[FATAL\]") {
        $color = $Colors.Error
        $icon = "ERR"
    }
    elseif ($LogLine -match "\[WARN\]") {
        $color = $Colors.Warning  
        $icon = "WRN"
    }
    elseif ($LogLine -match "\[DEBUG\]") {
        $color = $Colors.Debug
        $icon = "DBG"
    }
    elseif ($LogLine -match "ruler|[rul]|onclick|onmousemove") {
        $color = $Colors.Ruler
        $icon = "RUL"
        $Global:RulerEvents++
    }
    elseif ($LogLine -match "SNAPSHOT|ALERT|Memory|CPU") {
        $color = $Colors.Performance
        $icon = "PRF"
    }
    elseif ($LogLine -match "\[INFO\]|\[SUCCESS\]") {
        $color = $Colors.Success
        $icon = "INF"
    }
    
    # Format source file indicator
    $fileIndicator = ""
    if ($SourceFile -eq "performance_monitor.log") {
        $fileIndicator = "[PERF] "
    }
    elseif ($SourceFile -match "error") {
        $fileIndicator = "[ERR] "
    }
    elseif ($SourceFile -match "powershell") {
        $fileIndicator = "[PS] "
    }
    
    Write-Host "$prefix " -ForegroundColor $Colors.Timestamp -NoNewline
    Write-Host "[$icon] $fileIndicator" -ForegroundColor $color -NoNewline
    Write-Host $LogLine -ForegroundColor $color
    
    $Global:TotalLogs++
}

function Monitor-LogFile {
    param([string]$FilePath)
    
    if (-not (Test-Path $FilePath)) {
        return
    }
    
    # Get current file size
    $currentSize = (Get-Item $FilePath).Length
    $lastSize = $Global:LastPositions[$FilePath]
    
    if ($null -eq $lastSize) {
        # First time reading - start from end
        $Global:LastPositions[$FilePath] = $currentSize
        return
    }
    
    if ($currentSize -gt $lastSize) {
        # File has grown - read new content
        try {
            $stream = [System.IO.FileStream]::new($FilePath, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $stream.Seek($lastSize, [System.IO.SeekOrigin]::Begin) | Out-Null
            
            $reader = [System.IO.StreamReader]::new($stream)
            while (-not $reader.EndOfStream) {
                $line = $reader.ReadLine()
                if ($line -and (Test-RulerRelated $line)) {
                    Format-LogEntry $line (Split-Path $FilePath -Leaf)
                }
            }
            
            $reader.Close()
            $stream.Close()
            
            $Global:LastPositions[$FilePath] = $currentSize
        }
        catch {
            # Ignore file access errors during monitoring
        }
    }
    elseif ($currentSize -lt $lastSize) {
        # File was truncated or rotated
        $Global:LastPositions[$FilePath] = 0
    }
}

function Show-Statistics {
    $runtime = (Get-Date) - $Global:StartTime
    $runtimeStr = "{0:mm\:ss}" -f $runtime
    
    Write-Host ""
    Write-Host "--------------------------------------------------------------------------------" -ForegroundColor $Colors.Header
    Write-Host "MONITORING STATISTICS:" -ForegroundColor $Colors.Info
    Write-Host "   Runtime: $runtimeStr" -ForegroundColor $Colors.Info
    Write-Host "   Ruler Events: $($Global:RulerEvents)" -ForegroundColor $Colors.Ruler
    Write-Host "   Total Logs: $($Global:TotalLogs)" -ForegroundColor $Colors.Performance
    Write-Host "--------------------------------------------------------------------------------" -ForegroundColor $Colors.Header
}

# ==============================================================================
# MAIN EXECUTION
# ==============================================================================

try {
    $Global:StartTime = Get-Date
    
    # Initialize last positions
    foreach ($file in $LogFiles) {
        if (Test-Path $file) {
            $Global:LastPositions[$file] = (Get-Item $file).Length
        }
    }
    
    Show-Header
    
    # Main monitoring loop
    while ($true) {
        foreach ($logFile in $LogFiles) {
            Monitor-LogFile $logFile
        }
        
        # Update statistics every 10 seconds
        if (($Global:TotalLogs % 10) -eq 0 -and $Global:TotalLogs -gt 0) {
            Show-Statistics
        }
        
        Start-Sleep -Seconds $RefreshInterval
    }
}
catch [System.Management.Automation.PipelineStoppedException] {
    # Ctrl+C pressed
    Write-Host ""
    Write-Host "Log monitoring stopped by user" -ForegroundColor $Colors.Warning
}
catch {
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor $Colors.Error
}
finally {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor $Colors.Header
    Write-Host "                        RULER MONITOR REPORT                                   " -ForegroundColor $Colors.Header
    Write-Host "================================================================================" -ForegroundColor $Colors.Header
    
    $runtime = if ($Global:StartTime) { (Get-Date) - $Global:StartTime } else { New-TimeSpan }
    $runtimeStr = "{0:hh\:mm\:ss}" -f $runtime
    
    Write-Host "Final Statistics:" -ForegroundColor $Colors.Info
    Write-Host "   Total Runtime: $runtimeStr" -ForegroundColor $Colors.Info
    Write-Host "   Ruler Events Captured: $($Global:RulerEvents)" -ForegroundColor $Colors.Ruler
    Write-Host "   Total Log Entries: $($Global:TotalLogs)" -ForegroundColor $Colors.Performance
    Write-Host ""
    Write-Host "Ready to analyze ruler test results!" -ForegroundColor $Colors.Success
    Write-Host ""
}
