# گزارش تطبیق کامل منطق رسم سطوح - MT4 و MotiveWave

تاریخ: 2025-10-07
نسخه: 3.00 Final

## 📋 خلاصه تغییرات

تمامی تفاوت‌های منطق رسم سطوح بین MT4 و MotiveWave شناسایی و رفع شدند. حالا هر دو پلتفرم از **کاملاً همان منطق** استفاده می‌کنند.

---

## 🔍 تفاوت‌های شناسایی شده و رفع شده

### 1️⃣ افزودن تابع `ShouldDrawStep` به MT4

#### ❌ قبل:
```mql4
// هیچ فیلتری برای رسم سطوح وجود نداشت
// همه سطوح رسم می‌شدند
```

#### ✅ بعد:
```mql4
bool ShouldDrawStep(const int step) {
    // If Trigger lines are enabled, all steps are drawn
    if(inpShowTHLevels) return true;
    // Otherwise only steps that are multiples of 4 (structure highlights) are rendered
    return (step % 4 == 0);
}
```

**تطابق با Java:**
```java
private static boolean shouldDrawStep(Settings settings, int step) {
    if (settings.getBoolean(S_SHOW_TRIGGER_LEVELS)) return true;
    return step % 4 == 0;
}
```

✅ **حالا یکسان است!**

---

### 2️⃣ اضافه کردن چک `highestHigh` و `lowestLow` در SS/LS

#### ❌ قبل:
```mql4
// Above
while(drawnAbove < inpMaxTHLevelsAbove) {
    // فقط تعداد چک می‌شد، بدون محدودیت قیمت
}

// Below
while(drawnBelow < inpMaxTHLevelsBelow) {
    // فقط تعداد چک می‌شد، بدون محدودیت قیمت
}
```

#### ✅ بعد:
```mql4
// Above
while(drawnAbove < inpMaxTHLevelsAbove) {
    double priceLevel = midpointPrice + cumulative;
    
    // Check if we exceeded the highest high (matching Java logic)
    if(priceLevel > g_highestHigh) break;
    
    // Check if this step should be drawn (matching Java shouldDrawStep)
    if(!ShouldDrawStep(logicalStep)) {
        continue;
    }
    // ...
}

// Below
while(drawnBelow < inpMaxTHLevelsBelow) {
    double priceLevel = midpointPrice - cumulative;
    
    // Check if we went below the lowest low (matching Java logic)
    if(priceLevel < g_lowestLow) break;
    
    // Check if this step should be drawn (matching Java shouldDrawStep)
    if(!ShouldDrawStep(logicalStep)) {
        continue;
    }
    // ...
}
```

**تطابق با Java:**
```java
// Above (line 182)
if (priceLevel > highestHigh) break;

// Below (line 208)
if (priceLevel < lowestLow) break;

// Both use shouldDrawStep (lines 184, 210)
if (!shouldDrawStep(settings, logicalStep)) {
    continue;
}
```

✅ **حالا یکسان است!**

---

### 3️⃣ تصحیح محدودیت قیمت در M Levels

#### ❌ قبل:
```mql4
// Above
while(priceAbove <= 999999 && drawnAbove < inpMaxTHLevelsAbove) {
    // استفاده از عدد ثابت 999999 - نادرست!
}

// Below
while(priceBelow >= 0 && drawnBelow < inpMaxTHLevelsBelow) {
    // استفاده از 0 - نادرست!
}
```

#### ✅ بعد:
```mql4
// Above
while(priceAbove <= g_highestHigh && drawnAbove < inpMaxTHLevelsAbove) {
    // Check if this step should be drawn (matching Java shouldDrawStep)
    if(!ShouldDrawStep(logicalStep)) {
        logicalStep++;
        priceAbove += controlDistance;
        continue;
    }
    // ...
}

// Below
while(priceBelow >= g_lowestLow && drawnBelow < inpMaxTHLevelsBelow) {
    // Check if this step should be drawn (matching Java shouldDrawStep)
    if(!ShouldDrawStep(logicalStep)) {
        logicalStep++;
        priceBelow -= controlDistance;
        continue;
    }
    // ...
}
```

**تطابق با Java:**
```java
// Above (line 266)
while (priceAbove <= highestHigh && drawnAbove < maxAbove) {
    if (!shouldDrawStep(settings, logicalStep)) {
        logicalStep++;
        priceAbove += controlDistance;
        continue;
    }
    // ...
}

// Below (line 293)
while (priceBelow >= lowestLow && drawnBelow < maxBelow) {
    if (!shouldDrawStep(settings, logicalStep)) {
        logicalStep++;
        priceBelow -= controlDistance;
        continue;
    }
    // ...
}
```

✅ **حالا یکسان است!**

---

### 4️⃣ تصحیح محدودیت قیمت در M Equal Levels

#### ❌ قبل:
```mql4
// Above
while(price <= 999999 && step <= inpMaxTHLevelsAbove) {

// Below
while(price >= 0 && step <= inpMaxTHLevelsBelow) {
```

#### ✅ بعد:
```mql4
// Above
while(price <= g_highestHigh && step <= inpMaxTHLevelsAbove) {

// Below
while(price >= g_lowestLow && step <= inpMaxTHLevelsBelow) {
```

**تطابق با Java:**
```java
// Above (line 346)
while (price <= highestHigh && step <= maxAbove) {

// Below (similar pattern)
while (price >= lowestLow && step <= maxBelow) {
```

✅ **حالا یکسان است!**

---

## 📊 جدول مقایسه نهایی

| ویژگی | Java (MotiveWave) | MT4 (قبل) | MT4 (بعد) | وضعیت |
|------|------------------|-----------|----------|-------|
| محاسبه TH در Points | ✅ | ❌ | ✅ | ✅ |
| Fractal Values | ✅ | ✅ | ✅ | ✅ |
| SS/LS Formulas | ✅ | ✅ | ✅ | ✅ |
| تبدیل به Price Units | ✅ | ❌ | ✅ | ✅ |
| Timeframe Percentages | ✅ | ✅ | ✅ | ✅ |
| **shouldDrawStep Check** | ✅ | ❌ | ✅ | ✅ |
| **highestHigh/lowestLow Limit** | ✅ | ❌ | ✅ | ✅ |
| **Price Range Check در SS/LS** | ✅ | ❌ | ✅ | ✅ |
| **Price Range Check در M** | ✅ | ❌ | ✅ | ✅ |

---

## 🎯 منطق نهایی رسم سطوح (یکسان در هر دو پلتفرم)

### الگوریتم رسم SS/LS:

```
1. Initialize:
   - drawnCount = 0
   - logicalStep = 0
   - cumulative = 0

2. Loop:
   while (drawnCount < maxLevels) {
       a. Calculate distance for this step (SS or LS based on lsFirst and step number)
       b. Add distance to cumulative
       c. Increment logicalStep
       d. Calculate priceLevel = midpoint ± cumulative
       
       e. Check price limit:
          if (priceLevel > highestHigh) OR (priceLevel < lowestLow) -> BREAK
       
       f. Check if should draw:
          if (!shouldDrawStep(logicalStep)) -> CONTINUE
       
       g. Draw the level
       h. Increment drawnCount
   }
```

### الگوریتم shouldDrawStep:

```
shouldDrawStep(step):
    if (showTriggerLevels) return TRUE
    return (step % 4 == 0)
```

---

## 🔄 جریان کامل (End-to-End)

```
┌─────────────────────────────────────────────────┐
│  1. دریافت Daily Close Price                   │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│  2. محاسبه TH در Points                        │
│     thValue = (price × percentage ÷ 100) ÷ tick│
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│  3. محاسبه Fractal Values (در Points)         │
│     S = TH, P = TH/2, T = TH/4                 │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│  4. محاسبه SS/LS (در Points)                  │
│     SS = 2S - P, LS = 3S - 2P                  │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│  5. تبدیل به Price Units                       │
│     ssPrice = ssPoints × tickSize              │
│     lsPrice = lsPoints × tickSize              │
└────────────────┬────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────┐
│  6. رسم سطوح با شرایط:                         │
│     - چک highestHigh/lowestLow                 │
│     - چک shouldDrawStep (step % 4)             │
│     - چک maxLevelsAbove/Below                  │
└─────────────────────────────────────────────────┘
```

---

## ✅ چک‌لیست نهایی تطابق

- [x] فرمول‌های محاسباتی یکسان
- [x] واحدها (points vs price) سازگار
- [x] تبدیل واحدها صحیح
- [x] شرایط توقف یکسان (highestHigh/lowestLow)
- [x] فیلتر رسم سطوح یکسان (shouldDrawStep)
- [x] محدودیت تعداد سطوح یکسان (maxLevels)
- [x] منطق SS/LS alternating یکسان
- [x] منطق M every-3rd یکسان
- [x] Timeframe percentages یکسان

---

## 🎉 نتیجه‌گیری

**تمامی موارد بالا رفع شدند و حالا MT4 و MotiveWave از کاملاً همان منطق استفاده می‌کنند.**

### انتظارات:
1. **سطوح در قیمت‌های یکسان** رسم می‌شوند
2. **تعداد سطوح یکسان** است (با توجه به highestHigh/lowestLow)
3. **فیلتر structure/trigger** یکسان عمل می‌کند
4. **الگوی SS/LS alternating** دقیقاً مطابق است
5. **الگوی M (هر 3 تا C)** دقیقاً مطابق است

### برای تست:
```
1. باز کردن همان سیمبل در هر دو پلتفرم (مثلاً EURUSD)
2. انتخاب همان timeframe (مثلاً H1)
3. فعال کردن SS/LS mode در هر دو
4. چک کردن:
   ✓ قیمت اولین سطح SS بالای midpoint
   ✓ قیمت اولین سطح LS بالای midpoint
   ✓ تعداد کل سطوح رسم شده
   ✓ جایی که سطوح متوقف می‌شوند (نزدیک به highestHigh)
```

---

**پایان گزارش - همه چیز آماده برای تست است! 🚀**
