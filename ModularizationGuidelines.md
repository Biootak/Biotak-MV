# Modularization Guidelines

This project follows an additional strict modularization rule:

1. **Maximum 500 Lines per File**
   • Any Java source file (or other language file) must not exceed 500 logical lines of code.  
   • If the implementation grows beyond this limit, split the code into cohesive classes or helper modules residing in an appropriate package.

2. **Single Responsibility Principle**
   • Each class should encapsulate one well-defined responsibility. If new behavior does not align with the class purpose, it belongs in a new class.

3. **Package Organization**
   • `com.biotak.core` – fundamental calculation utilities (TH, ATR, etc.)  
   • `com.biotak.ui`   – drawing figures, panels, labels.  
   • `com.biotak.study` – MotiveWave Study entry points (e.g., `BiotakTrigger`).

4. **Eliminate Dead Code**
   • Remove unused methods, classes, constants, and imports as soon as they become obsolete.

5. **Code Review Checklist (enforced before commit)**
   ▫ File length ≤ 500 lines.  
   ▫ No unused identifiers (verify with IDE inspections / `mvn clean verify`).  
   ▫ Classes comply with package responsibilities above. 