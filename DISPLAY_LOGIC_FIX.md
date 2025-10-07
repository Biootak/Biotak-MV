# اصلاح نحوه نمایش سطوح - تطبیق کامل با Java

تاریخ: 2025-10-07  
**مشکل مهم یافت و برطرف شد! ✅**

---

## ⚠️ مشکل شناسایی شده

نحوه تصمیم‌گیری برای نمایش سطوح در MT4 و Java یکسان نبود!

###قبل از اصلاح (MT4):
```mql4
bool ShouldDrawStep(const int step) {
    if(inpShowTHLevels) return true;  // ❌ فقط یک چک ساده
    return (step % 4 == 0);
}
```

**مشکل**:
- فقط `inpShowTHLevels` چک می‌شد
- تفاوتی بین Structure و Trigger قائل نمی‌شد
- سطوح 4, 16, 32, 64, 128 به درستی فیلتر نمی‌شدند

---

## ✅ راه‌حل (مطابق Java)

### Java Logic (LevelDrawer.java):
```java
public static PathInfo getPathForLevel(Settings settings, int stepCount) {
    // First: Check structure levels (با اولویت از بالا به پایین)
    if (settings.getBoolean(S_SHOW_STRUCTURE_LINES)) {
        if (stepCount % 128 == 0 && settings.getBoolean(S_SHOW_STRUCT_L5)) return ...L5...;
        if (stepCount % 64 == 0 && settings.getBoolean(S_SHOW_STRUCT_L4)) return ...L4...;
        if (stepCount % 32 == 0 && settings.getBoolean(S_SHOW_STRUCT_L3)) return ...L3...;
        if (stepCount % 16 == 0 && settings.getBoolean(S_SHOW_STRUCT_L2)) return ...L2...;
        if (stepCount % 4 == 0 && settings.getBoolean(S_SHOW_STRUCT_L1)) return ...L1...;
    }
    
    // Second: Check trigger (اگر فعال باشد، همه سطوح)
    if (settings.getBoolean(S_SHOW_TRIGGER_LEVELS)) {
        return settings.getPath(S_TRIGGER_PATH);
    }
    
    // None: هیچ سطحی رسم نشود
    return null;
}
```

### MT4 Fixed (ExtendedDrawingFunctions.mqh):
```mql4
bool ShouldDrawStep(const int step) {
    // First check structure levels (matching Java getPathForLevel)
    if(inpShowStructureTHLines) {
        if(step % 128 == 0 && inpShowStructureTHLineLevel5) return true;
        if(step % 64 == 0 && inpShowStructureTHLineLevel4) return true;
        if(step % 32 == 0 && inpShowStructureTHLineLevel3) return true;
        if(step % 16 == 0 && inpShowStructureTHLineLevel2) return true;
        if(step % 4 == 0 && inpShowStructureTHLineLevel1) return true;
    }
    
    // If Trigger lines are enabled, all steps are drawn (matching Java line 139)
    if(inpShowTriggerTHLevels) return true;
    
    // Don't draw anything if neither structure nor trigger lines are enabled
    return false;
}
```

---

## 📊 جدول مقایسه قبل/بعد

| شرایط | قبل (اشتباه) | بعد (صحیح) | Java |
|-------|-------------|-----------|------|
| Structure ON + Level1 ON | همه | فقط 4,8,12,16,... | فقط 4,8,12,16,... ✅ |
| Structure ON + Level2 ON | همه | فقط 16,32,48,... | فقط 16,32,48,... ✅ |
| Structure ON + Level3 ON | همه | فقط 32,64,96,... | فقط 32,64,96,... ✅ |
| Structure ON + Level4 ON | همه | فقط 64,128,... | فقط 64,128,... ✅ |
| Structure ON + Level5 ON | همه | فقط 128,256,... | فقط 128,256,... ✅ |
| Trigger ON | همه | همه | همه ✅ |
| Structure OFF + Trigger OFF | هر 4 تا | هیچ | هیچ ✅ |

---

## 🎯 نحوه کار منطق جدید

### حالت 1: فقط Structure Level 1 فعال
```
Step 1: بررسی می‌شود
  - 1 % 128 ≠ 0 → ❌
  - 1 % 64 ≠ 0 → ❌
  - 1 % 32 ≠ 0 → ❌
  - 1 % 16 ≠ 0 → ❌
  - 1 % 4 ≠ 0 → ❌
  - Trigger OFF → ❌
  → نتیجه: رسم نمی‌شود

Step 4: بررسی می‌شود
  - 4 % 4 == 0 && Level1 ON → ✅
  → نتیجه: رسم می‌شود
```

### حالت 2: Trigger فعال
```
Step 1: بررسی می‌شود
  - Trigger ON → ✅
  → نتیجه: رسم می‌شود

Step 2: بررسی می‌شود
  - Trigger ON → ✅
  → نتیجه: رسم می‌شود

همه سطوح رسم می‌شوند ✅
```

### حالت 3: Structure Level 2 و Level 4 فعال
```
Step 16: بررسی می‌شود
  - 16 % 64 ≠ 0 → ❌
  - 16 % 16 == 0 && Level2 ON → ✅
  → نتیجه: رسم می‌شود

Step 64: بررسی می‌شود
  - 64 % 64 == 0 && Level4 ON → ✅
  → نتیجه: رسم می‌شود
```

---

## 🔍 تفاوت‌های کلیدی

### قبل:
1. ❌ فقط `inpShowTHLevels` چک می‌شد (یک boolean ساده)
2. ❌ تفکیک بین structure levels وجود نداشت
3. ❌ Trigger و Structure مجزا نبودند

### بعد:
1. ✅ هر Level جداگانه چک می‌شود (L1, L2, L3, L4, L5)
2. ✅ اولویت از بالا به پایین (128 → 64 → 32 → 16 → 4)
3. ✅ Trigger همه سطوح را نمایش می‌دهد
4. ✅ اگر هیچکدام فعال نباشد، هیچ سطحی رسم نمی‌شود

---

## 📝 تاثیر بر TH_STEP mode

در حالت `TH_STEP`، تابع `DrawTHLevels` در `LabelFunctions.mqh` نیز از همین منطق استفاده می‌کند (خطوط 100-252) و سطوح را طبق structure levels فیلتر می‌کند.

**این اصلاح فقط در `ExtendedDrawingFunctions.mqh` لازم بود** زیرا:
- `DrawSSLSLevels` از `ShouldDrawStep` استفاده می‌کند
- `DrawMLevels` از `ShouldDrawStep` استفاده می‌کند  
- `DrawCustomStepLevels` (E/TP) از `ShouldDrawStep` استفاده نمی‌کند (همه را نمایش می‌دهد)

---

## ✅ نتیجه

**حالا نحوه نمایش سطوح در MT4 دقیقاً مانند Java است!**

این یک اصلاح مهم بود که کیفیت نمایش سطوح را به طرز چشمگیری بهبود می‌دهد.

---

**تاریخ اصلاح**: 2025-10-07  
**وضعیت**: ✅ اصلاح کامل شد
