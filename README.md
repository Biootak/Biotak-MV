# Biotak Trigger TH3 - Clean Version

A MotiveWave trading indicator that provides technical analysis levels based on historical volatility and price action.

## Features

- **TH Level Calculation**: Calculate key trading levels based on timeframe analysis
- **Multiple Step Modes**: 
  - Equal TH Steps
  - SS/LS Steps  
  - Control Steps
  - M-Steps
- **Interactive Interface**: Info panel with real-time calculations
- **Leg Ruler**: Measure price movements and find fractal matches
- **Theme Support**: Light/Dark theme with adaptive colors
- **Performance Optimized**: Clean, efficient codebase

## Project Structure

```
src/
├── com/biotak/
│   ├── BiotakTrigger.java      # Main indicator class
│   ├── config/                 # Configuration classes
│   ├── core/                   # Core calculation logic
│   ├── debug/                  # Logging system
│   ├── enums/                  # Enumerations
│   ├── ui/                     # User interface components
│   └── util/                   # Utility classes
```

## Build

```bash
javac -cp "lib/mwave_sdk.jar" -d "build/classes" src/com/biotak/*.java src/com/biotak/*/*.java
jar cfm "build/biotak-clean.jar" "build/MANIFEST.MF" -C "build/classes" .
```

## Installation

1. Copy `biotak-clean.jar` to MotiveWave's `extensions` folder
2. Restart MotiveWave
3. Add the indicator from Biotak menu

## Version

3.0-Clean - Optimized and cleaned version with dead code removed.
