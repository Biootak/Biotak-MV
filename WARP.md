# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a Java-based financial trading indicator called **Biotak Trigger TH3** for the MotiveWave trading platform. It calculates and displays key horizontal price levels based on historical volatility and fractal price action analysis.

## Build Commands

### Standard Build (Default)
```powershell
.\build.ps1
# or on Unix
./build.sh
```
This compiles all Java sources and creates a JAR file that gets deployed to the MotiveWave Extensions directory.

### Development Mode (Hot Reload)
```powershell
.\build.ps1 dev
# or on Unix
./build.sh dev
```
Deploys compiled classes directly to MotiveWave for hot-reloading during development.

### Clean Build
```powershell
.\build.ps1 clean
# or on Unix
./build.sh clean
```
Removes all build artifacts and performs a fresh build.

### Test Mode
```powershell
.\build.ps1 test
# or on Unix
./build.sh test
```
Compiles and runs any available unit tests using JUnit.

### Release Build
```powershell
.\build.ps1 release
# or on Unix
./build.sh release
```
Creates an optimized release version.

## Development Environment

- **Java Version**: JDK 24 (hardcoded requirement for MotiveWave SDK compatibility)
- **SDK Dependency**: `lib/mwave_sdk.jar` (MotiveWave SDK)
- **Build Output**: `build/biotak.jar`
- **Deployment**: Automatically deploys to `C:/Users/Fatemehkh/MotiveWave Extensions/lib/`

## Architecture Overview

### Core Components

#### Main Study Class
- **`BiotakTrigger.java`**: The main indicator class extending MotiveWave's `Study` class
  - Manages indicator lifecycle, UI panels, and level calculations
  - Implements interactive ruler functionality for price measurements
  - Handles multiple step calculation modes (TH, SS/LS, M-Step, E-Step, TP-Step)

#### Configuration System
- **`BiotakConfig.java`**: Centralized configuration manager with singleton pattern
  - Thread-safe property management with validation
  - Environment-specific configuration loading
  - Hot-reload capability for configuration changes

#### Core Calculation Engine
- **`FractalCalculator.java`**: Fractal-based price level calculations
  - Calculates Structure, Pattern, and Trigger values using fractal mathematics
  - ATR (Average True Range) calculations for multiple timeframes
  - Short Step (SS) and Long Step (LS) formulas: `SS = (2*S) - P`, `LS = (3*S) - (2*P)`

#### Advanced Logging System
- **`AdvancedLogger.java`**: Professional multi-level logging with categories
  - File and console output with color coding
  - Performance tracking and log rotation
  - Specialized logging for different components (Ruler, Performance, Scripts)

### Package Structure

```
src/com/biotak/
├── BiotakTrigger.java          # Main indicator class
├── config/                     # Configuration management
│   ├── BiotakConfig.java       # Central config manager
│   ├── LoggingConfiguration.java
│   ├── SettingsRepository.java
│   └── SettingsService.java
├── core/                       # Core calculation logic
│   ├── FractalCalculator.java  # Fractal price calculations
│   └── RulerService.java       # Interactive ruler logic
├── debug/                      # Logging and debugging
│   └── AdvancedLogger.java     # Professional logging system
├── enums/                      # Type definitions
│   ├── StepCalculationMode.java # Calculation mode types
│   ├── PanelPosition.java
│   ├── RulerState.java
│   └── [other enums]
├── ui/                         # User interface components
│   ├── InfoPanel.java          # Main info display panel
│   ├── CustomPriceLine.java    # Interactive price line
│   ├── LevelDrawer.java        # Level rendering
│   ├── ThemeManager.java       # Theme system
│   └── [other UI components]
└── util/                       # Utility classes
    ├── Constants.java          # Global constants and mappings
    ├── TimeframeUtil.java      # Timeframe calculations
    ├── OptimizedCalculations.java # Performance-optimized math
    ├── CacheManager.java       # Memory and cache management
    └── [other utilities]
```

### Key Architectural Patterns

#### Fractal Price Analysis
The indicator implements a sophisticated fractal-based approach to price level calculation:
- **Structure (S)**: Based on current timeframe
- **Pattern (P)**: One fractal level down (S/2)
- **Trigger (T)**: Two fractal levels down (P/2)
- **Step Calculations**: Multiple calculation modes including SS/LS and M-Step algorithms

#### Multi-Timeframe ATR Integration
- Calculates ATR for current, pattern, and trigger timeframes
- Uses timeframe-specific ATR periods (24 for minutes, 22 for daily, 52 for weekly, 12 for monthly)
- Implements "Live ATR" for real-time volatility measurement

#### Performance Optimization
- **Concurrent Maps**: Thread-safe caching with size limits (1000 entries, cleanup at 800)
- **Object Pools**: Reusable object patterns to reduce GC pressure
- **Optimized Math**: FastMath utilities for performance-critical calculations
- **Lazy Initialization**: Components load on-demand

#### Theme and Adaptation System
- **Auto-detection**: Automatically adapts to chart background (light/dark)
- **Adaptive Colors**: Dynamic color adjustment based on market conditions
- **Configurable Themes**: Manual override options for light/dark themes

## Configuration Files

- **`biotak.properties`**: Main configuration file with performance, UI, and calculation settings
- Settings automatically sync between file system and MotiveWave's study settings

## Logging and Debugging

The logging system creates multiple log files in `logs/` directory:
- `biotak_main.log`: General application logs
- `biotak_errors.log`: Error-specific logs
- `biotak_debug.log`: Debug information
- `biotak_performance.log`: Performance metrics
- `biotak_ruler.log`: Interactive ruler operations

Log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL

## Testing Single Components

To test individual calculation components:
```java
// Test fractal calculations
double[] values = FractalCalculator.calculateFractalValues(barSize, thValue);

// Test ATR calculations  
double atr = FractalCalculator.calculateATR(dataSeries);

// Test configuration
BiotakConfig config = BiotakConfig.getInstance();
config.printConfiguration();
```

## Memory and Performance Considerations

- Maps are limited to 1000 entries with automatic cleanup at 800 entries
- Object pooling is used for frequently created UI components
- Logging includes performance tracking for bottleneck identification
- Configuration allows tuning of cache sizes and thread pools

## Key Integration Points

- **MotiveWave SDK**: Core dependency for all chart interactions
- **Study Settings**: All configuration syncs with MotiveWave's settings system
- **Drawing Context**: All UI rendering works within MotiveWave's drawing framework
- **Data Series**: Real-time price data integration for calculations
