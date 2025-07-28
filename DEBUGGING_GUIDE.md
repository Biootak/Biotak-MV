# Biotak Trigger - Advanced Debugging Guide

## Enhanced Logging System

### Log Levels Available
- **DEBUG**: Most detailed logging for development debugging
- **INFO**: General information about operations
- **WARN**: Warning messages (default level)
- **ERROR**: Error messages only

### Ruler-Specific Logging Methods

The enhanced logging system includes specialized ruler debugging methods:

#### 1. State Transition Logging
```java
logRulerStateTransition(methodName, oldState, newState, additionalInfo)
```
- Tracks ruler state changes with clear before/after states
- Format: `[method] Ruler state transition: INACTIVE → WAITING_FOR_START | Enabling ruler mode`

#### 2. Debug Logging
```java
logRulerDebug(methodName, message)
```
- Detailed debug information only shown when DEBUG level is active
- Format: `[method] message`

#### 3. Info Logging
```java
logRulerInfo(methodName, message)
```
- Important information messages
- Format: `[method] message`

#### 4. Error Logging
```java
logRulerError(methodName, message)
```
- Error conditions and failures
- Format: `[method] ERROR: message`

## How to Enable Debug Logging

### Method 1: Code Level (Temporary)
In the constructor, the log level is set to DEBUG:
```java
Logger.setLogLevel(LogLevel.DEBUG);
```

### Method 2: Settings (Runtime)
In MotiveWave, go to:
1. Study Settings → Ruler tab
2. Set "Log Level" to "DEBUG"

## Debugging the Ruler Issue

### Current Logs Show
Based on your log output, the ruler functionality appears to be working through these stages:

1. **Ruler Button Click** → State changes to `WAITING_FOR_START`
2. **First Click** → State changes to `WAITING_FOR_END` 
3. **Second Click** → State changes to `ACTIVE`

### Key Debug Points to Monitor

#### 1. Ruler State Management
Monitor these log messages:
- `[handleRulerButtonClick] Ruler state transition: X → Y`
- `[onClick] Ruler state is WAITING_FOR_START`
- `[onClick] Start point selection in progress`
- `[onClick] End point selection in progress`

#### 2. Component Existence Checks
Look for these debug messages:
- `InfoPanel exists: true/false`
- `RulerStartResize exists: true/false`
- `RulerEndResize exists: true/false`
- `RulerFigure exists: true/false`
- `DrawContext available: true/false`

#### 3. Drawing Process
Monitor the drawing chain:
- `Calling drawFigures to redraw with lastIdx=X`
- `drawFigures completed`

### Common Issues & Solutions

#### Issue 1: Ruler Not Appearing
**Debug Steps:**
1. Check if `rulerState` reaches `ACTIVE`
2. Verify `rulerStartResize` and `rulerEndResize` are not null
3. Confirm `rulerFigure` is created and added to chart

**Log Pattern to Look For:**
```
[handleRulerButtonClick] Ruler state transition: INACTIVE → WAITING_FOR_START
[onClick] Start point selected, waiting for end point
[onClick] End point selected, ruler completed  
[drawFigures] Adding ruler figure to chart
```

#### Issue 2: Button Not Responding
**Debug Steps:**
1. Check if InfoPanel exists and button bounds are correct
2. Verify click coordinates are within button area
3. Monitor DrawContext availability

**Log Pattern to Look For:**
```
[onClick] Processing click at java.awt.Point[x=X,y=Y]
[handleRulerButtonClick] Method called - current showRuler=false, rulerState=INACTIVE
[handleRulerButtonClick] Method completed - final rulerState=WAITING_FOR_START
```

### Testing Procedure

1. **Enable Debug Logging**
   ```java
   Logger.setLogLevel(LogLevel.DEBUG);
   ```

2. **Test Ruler Activation**
   - Click the ruler button in InfoPanel
   - Look for state transition logs
   - Verify component creation logs

3. **Test Point Selection**
   - Click first point on chart
   - Look for coordinate conversion logs
   - Verify start point creation

4. **Test Ruler Completion**
   - Click second point on chart
   - Look for ruler figure creation logs
   - Verify final drawing process

### Log File Location
```
C:\Users\fatemeh\IdeaProject\Biotak\biotak_log.txt
```

### Real-time Log Monitoring
Use PowerShell to monitor logs in real-time:
```powershell
Get-Content "C:\Users\fatemeh\IdeaProject\Biotak\biotak_log.txt" -Wait -Tail 20
```

## Current Status Analysis

From your recent logs, I can see:
- The calculation table is being generated correctly
- The system is processing timeframe calculations properly
- The ruler state management system is in place

The next step is to run the updated code in MotiveWave and monitor the debug logs when you:
1. Click the ruler button
2. Select the first point
3. Select the second point

This will help identify exactly where the ruler functionality is failing.

## Additional Notes

- The logging system includes throttling to prevent spam
- Log files are automatically rotated when they exceed 5MB
- All ruler operations are now logged with clear method names and state information
- The enhanced logging maintains performance while providing detailed debugging information

Remember: The key is to follow the ruler state transitions in the logs to identify exactly where the process breaks down.
