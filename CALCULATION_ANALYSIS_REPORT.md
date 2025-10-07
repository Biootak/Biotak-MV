# گزارش تحلیل کامل محاسبات Biotak Trigger - MT4 vs MotiveWave

تاریخ: 2025-10-07
نسخه: 3.00

## 📊 خلاصه اجرایی

تحلیل دقیق و جامع محاسبات در هر دو پلتفرم MT4 و MotiveWave (Java) نشان می‌دهد که **تمامی فرمول‌ها و محاسبات اصلی یکسان هستند**. مشکل قبلی در واحد محاسبات (points vs price units) برطرف شده است.

## 🔍 نتایج بررسی (دید کل‌نگر)

### ✅ موارد تطابق کامل:

1. **محاسبه TH در Points**: هر دو پلتفرم از فرمول `(price × percentage ÷ 100) ÷ tickSize` استفاده می‌کنند
2. **محاسبه Fractal Values**: Structure = TH, Pattern = TH/2, Trigger = TH/4
3. **محاسبه SS/LS**: SS = 2S - P و LS = 3S - 2P
4. **تبدیل به Price Units**: هر دو از `points × tickSize` استفاده می‌کنند
5. **Timeframe Percentages**: مقادیر دقیقاً یکسان (0.02, 0.04, 0.08, ...)

---

## 📐 بررسی جزء‌نگر فرمول‌ها

### 1. محاسبه TH اصلی (calculateTHPoints)

#### Java (OptimizedCalculations.java, lines 114-123):
```java
public static double calculateTHOptimized(Instrument instrument, double basePrice, double percentage) {
    if (basePrice <= 0 || percentage <= 0) return 0;
    
    double tickSize = instrument.getTickSize();
    if (tickSize <= 0) return 0;
    
    // Calculate TH in price units
    double thStepPriceUnits = (basePrice * percentage) / 100.0;
    
    // Convert to points by dividing by tick size
    return thStepPriceUnits / tickSize;
}
```

#### MT4 (THCalculations.mqh, lines 78-87):
```mql4
double CalculateTHPoints(const double price, const int digits, const double percentage) {
    if(price <= 0 || percentage <= 0) return 0;
    
    static double s_pointValue = 0.0;
    if(s_pointValue == 0.0) s_pointValue = GetSymbolPoint();
    if(s_pointValue == 0.0) return 0;
    
    // Calculate TH in price units
    double thStepPriceUnits = NormalizeDouble((price * percentage) / 100.0, Digits);
    
    // Convert to points by dividing by point value
    return thStepPriceUnits <= 0 ? 0 : NormalizeDouble(thStepPriceUnits / s_pointValue, 1);
}
```

**نتیجه**: ✅ یکسان - فقط MT4 از NormalizeDouble برای دقت بیشتر استفاده می‌کند

---

### 2. محاسبه Fractal Values

#### Java (FractalCalculator.java, lines 18-28):
```java
public static double[] calculateFractalValues(BarSize barSize, double thValue) {
    double structureValue = thValue;                // S = TH
    double patternValue = structureValue / 2.0;     // P = S / 2 = TH / 2
    double triggerValue = patternValue / 2.0;       // T = P / 2 = TH / 4
    
    return new double[] {structureValue, patternValue, triggerValue};
}
```

#### MT4 (THCalculations.mqh, lines 177-186):
```mql4
void CalculateFractalValues(const double currentTH, double &structureValue, 
                           double &patternValue, double &triggerValue) {
    structureValue = currentTH;                    // S = TH
    patternValue = structureValue * 0.5;           // P = S × 0.5 = TH / 2
    triggerValue = structureValue * 0.25;          // T = S × 0.25 = TH / 4
}
```

**نتیجه**: ✅ یکسان - هر دو از همان فرمول‌های fractal استفاده می‌کنند

---

### 3. محاسبه Short Step (SS) و Long Step (LS)

#### Java (FractalCalculator.java):
```java
// Line 38-39
public static double calculateShortStep(double structureValue, double patternValue) {
    return (2 * structureValue) - patternValue;  // SS = 2S - P
}

// Line 49-50
public static double calculateLongStep(double structureValue, double patternValue) {
    return (3 * structureValue) - (2 * patternValue);  // LS = 3S - 2P
}
```

#### MT4 (THCalculations.mqh):
```mql4
// Lines 17-24
double CalculateShortStep(const double structureTH, const double patternTH) {
    s_lastShortStep = (2.0 * structureTH) - patternTH;  // SS = 2S - P
    return s_lastShortStep;
}

// Lines 7-14
double CalculateLongStep(const double structureTH, const double patternTH) {
    s_lastLongStep = (3.0 * structureTH) - (2.0 * patternTH);  // LS = 3S - 2P
    return s_lastLongStep;
}
```

**نتیجه**: ✅ یکسان - فرمول‌های SS و LS دقیقاً مشابه هستند

---

### 4. تبدیل Points به Price Units برای رسم

#### Java (LevelDrawer.java, line 87):
```java
double pointValue = series.getInstrument().getTickSize();
double stepPrice = thStepInPoints * pointValue;  // Convert points to price units
double priceLevelAbove = midpointPrice + stepPrice;  // Add to midpoint
```

#### MT4 (EventHandlers.mqh, lines 148-153):
```mql4
double pointSize = Point();  // Get tick size
double structureValuePrice = structureValue * pointSize;  // Convert to price
double patternValuePrice = patternValue * pointSize;
double triggerValuePrice = triggerValue * pointSize;
double shortStepPrice = shortStep * pointSize;
double longStepPrice = longStep * pointSize;
```

**نتیجه**: ✅ یکسان - هر دو از ضرب در tick/point size استفاده می‌کنند

---

### 5. Timeframe Percentages

#### Java (Constants.java, lines 25-33):
```java
map.put("M1", 0.02);
map.put("M4", 0.04);
map.put("M16", 0.08);
map.put("H1+M4", 0.16);
map.put("H4+M16", 0.32);
map.put("H17+M4", 0.64);
map.put("D2+H20+M16", 1.28);
map.put("D11+H9+M4", 2.56);
map.put("D45+H12+M16", 5.12);
```

#### MT4 (ConstantsAndEnums.mqh, lines 13-15):
```mql4
const double MODIFIED_FRACTAL_PERCENTAGES[] = {
    0.02, 0.04, 0.08, 0.16, 0.32, 0.64, 1.28, 2.56, 5.12
};
```

**نتیجه**: ✅ یکسان - درصدها دقیقاً مطابق هستند

---

## 🔧 تغییرات انجام شده در MT4 (EventHandlers.mqh)

### قبل از تغییر (خطا):
```mql4
// اشتباه: استفاده از CalculateTH که price unit برمی‌گرداند
double thValue = CalculateTH(dailyClosePrice, digits, timeframePercentage);
CalculateFractalValues(thValue, structureValue, patternValue, triggerValue);

// اشتباه: مستقیم استفاده می‌شد بدون تبدیل
DrawSSLSLevels(objectPrefix, midpointPrice, ssValue, lsValue, inpLSFirst);
```

### بعد از تغییر (صحیح):
```mql4
// ✅ صحیح: استفاده از CalculateTHPoints که points برمی‌گرداند
double thValue = CalculateTHPoints(dailyClosePrice, digits, timeframePercentage);
CalculateFractalValues(thValue, structureValue, patternValue, triggerValue);

// ✅ صحیح: تبدیل به price units قبل از رسم
double pointSize = Point();
double ssValuePrice = ssValue * pointSize;
double lsValuePrice = lsValue * pointSize;
DrawSSLSLevels(objectPrefix, midpointPrice, ssValuePrice, lsValuePrice, inpLSFirst);
```

---

## 📈 جریان کامل محاسبات (Flow Diagram)

```
1. محاسبه TH در Points
   ├─► Java: thValue = calculateTHPoints(instrument, price, percentage)
   └─► MT4:  thValue = CalculateTHPoints(price, digits, percentage)
   
2. محاسبه Fractal Values (همه در Points)
   ├─► Structure = thValue
   ├─► Pattern = thValue / 2
   └─► Trigger = thValue / 4
   
3. محاسبه SS/LS (در Points)
   ├─► SS = 2 × Structure - Pattern
   └─► LS = 3 × Structure - 2 × Pattern
   
4. تبدیل به Price Units برای رسم
   ├─► Java: stepPrice = stepInPoints × tickSize
   └─► MT4:  stepPrice = stepInPoints × Point()
   
5. رسم سطوح
   ├─► levelPrice = midpointPrice ± stepPrice
   └─► Draw horizontal line at levelPrice
```

---

## 🎯 نتیجه‌گیری نهایی

### محاسبات:
- ✅ فرمول‌های TH یکسان است
- ✅ فرمول‌های Fractal Values یکسان است
- ✅ فرمول‌های SS/LS یکسان است
- ✅ تبدیل واحدها صحیح است
- ✅ Timeframe percentages مطابق است

### انتظار:
با این تغییرات، **سطوح رسم شده در MT4 باید دقیقاً با MotiveWave مطابقت داشته باشند** (با احتساب تفاوت‌های ناچیز rounding).

### گام بعدی:
تست عملی روی چارت واقعی در هر دو پلتفرم و مقایسه بصری سطوح رسم شده.

---

## 📝 نکات مهم برای تست

1. **همان symbol را باز کنید**: مثلاً EURUSD در هر دو پلتفرم
2. **همان timeframe را انتخاب کنید**: مثلاً M15 یا H1
3. **همان daily close price را استفاده کنید**: چک کنید که هر دو از همان قیمت استفاده می‌کنند
4. **SS/LS mode را فعال کنید**: در هر دو پلتفرم Step Calculation Mode را روی SS_LS_STEP بگذارید
5. **Same basis type**: مثلاً STRUCTURE را در هر دو انتخاب کنید
6. **مقایسه visual**: سطوح اول و دوم SS/LS را با هم مقایسه کنید

---

تهیه شده توسط: Agent Mode (AI Assistant)
