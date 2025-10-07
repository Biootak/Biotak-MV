# گزارش مشکلات باقی‌مانده در MT4 - نیاز به تکمیل

تاریخ: 2025-10-07
وضعیت: نیاز به اصلاح

---

## ⚠️ مشکلات شناسایی شده

### 1. E_STEP و TP_STEP به درستی کار نمی‌کنند ❌

**مکان**: `EventHandlers.mqh`, خطوط 221-235

**مشکل**:
```mql4
case E_STEP:
case TP_STEP:
{
    double eValue = CalculateEStep(thValue);       // محاسبه می‌شود
    double finalValue = ...;                        // محاسبه می‌شود
    
    DrawTHLevels(objectPrefix, dailyClosePrice);   // ❌ استفاده نمی‌شود!
    break;
}
```

`finalValue` محاسبه می‌شود اما هرگز به `DrawTHLevels` پاس داده نمی‌شود!

**راه‌حل پیشنهادی 1** (ساده):
- تابع جدیدی مثل `DrawETPLevels` ایجاد کنید که `finalValue` را به عنوان پارامتر بگیرد
- مشابه `DrawSSLSLevels` و `DrawMLevels` عمل کند

**راه‌حل پیشنهادی 2** (کامل):
- `DrawTHLevels` را بروزرسانی کنید تا یک پارامتر اختیاری `customStepSize` داشته باشد
- اگر این پارامتر داده شود، از آن استفاده کند، وگرنه محاسبه عادی انجام دهد

---

### 2. GetSymbolPoint() بررسی نشده ⚠️

**مکان**: باید چک شود که آیا این تابع درست tick size را برمی‌گرداند

**بررسی لازم**:
```mql4
// آیا این برابر است با:
double Point()  // یا
double MarketInfo(Symbol(), MODE_TICKSIZE)
```

---

### 3. Midpoint calculation - بررسی نشده ⚠️

**MT4** (EventHandlers.mqh, خط 174 و LabelFunctions.mqh, خط 88):
```mql4
midpointPrice = (g_highestHigh + g_lowestLow) / 2.0;
```

**Java** (LevelDrawer.java, خط 57):
```java
return (high + low) / 2.0;
```

✅ **به نظر یکسان است** - اما باید مطمئن شویم که `g_highestHigh` و `g_lowestLow` درست محاسبه می‌شوند.

---

### 4. Historical High/Low calculation - بررسی نشده ⚠️

**سوال کلیدی**: آیا MT4 و Java از همان محدوده زمانی برای محاسبه High/Low استفاده می‌کنند؟

**بررسی لازم**:
- در MT4: چطور `g_highestHigh` و `g_lowestLow` محاسبه می‌شوند؟
- در Java: چطور `highestHigh` و `lowestLow` محاسبه می‌شوند؟
- آیا هر دو از همان تعداد کندل استفاده می‌کنند؟

---

### 5. Custom Price handling - بررسی نشده ⚠️

**MT4**: از `g_customTHStartPrice` و global variables استفاده می‌کند
**Java**: از `S_CUSTOM_PRICE` setting استفاده می‌کند

**بررسی لازم**:
- آیا هر دو به همان روش عمل می‌کنند؟
- آیا وقتی custom price تنظیم می‌شود، نتایج یکسان است؟

---

### 6. Daily Close Price - بررسی نشده ⚠️

**MT4**: `GetPriceForPreviousDay(inpTHPriceType)`
**Java**: نیاز به بررسی دقیق

**بررسی لازم**:
- آیا هر دو از همان روش برای گرفتن daily close استفاده می‌کنند؟
- آیا "previous day" در هر دو همان معنی را دارد؟

---

## ✅ موارد تکمیل شده

- [x] محاسبه TH در Points
- [x] Fractal Values
- [x] SS/LS Formulas
- [x] تبدیل Points به Price Units
- [x] Timeframe Percentages
- [x] Price Limits (highestHigh/lowestLow) در رسم سطوح
- [x] shouldDrawStep logic
- [x] DrawTHLevels برای TH_STEP mode
- [x] DrawSSLSLevels برای SS_LS_STEP mode
- [x] DrawMLevels برای M_STEP mode

---

## 🔧 اقدامات پیشنهادی

### اولویت 1 (حیاتی):
1. **رفع مشکل E_STEP و TP_STEP**
   - ایجاد تابع `DrawCustomStepLevels` یا اصلاح `DrawTHLevels`

### اولویت 2 (مهم):
2. **بررسی GetSymbolPoint()**
   - مطمئن شوید که tick size را صحیح برمی‌گرداند

3. **بررسی Historical High/Low**
   - مقایسه منطق محاسبه در هر دو پلتفرم

### اولویت 3 (توصیه‌شده):
4. **تست Custom Price**
   - بررسی عملی در هر دو پلتفرم

5. **تست Daily Close Price**
   - مطمئن شوید هر دو از همان قیمت استفاده می‌کنند

---

## 📝 کد پیشنهادی برای رفع مشکل E/TP

```mql4
// در ExtendedDrawingFunctions.mqh
void DrawCustomStepLevels(const string objectPrefix, const double midpointPrice, 
                         const double stepSizePrice)
{
    if(stepSizePrice <= 0) {
        Print("DrawCustomStepLevels: Invalid step size");
        return;
    }
    
    // Draw levels above midpoint
    int drawnAbove = 0;
    double priceAbove = midpointPrice + stepSizePrice;
    
    while(priceAbove <= g_highestHigh && drawnAbove < inpMaxTHLevelsAbove) {
        string levelName = objectPrefix + "CustomStep_Above_" + IntegerToString(drawnAbove + 1);
        
        if(ObjectFind(0, levelName) < 0) {
            ObjectCreate(0, levelName, OBJ_HLINE, 0, 0, priceAbove);
        }
        
        ObjectSetInteger(0, levelName, OBJPROP_COLOR, inpTriggerTHLevelColor);
        ObjectSetInteger(0, levelName, OBJPROP_STYLE, inpTriggerTHLevelStyle);
        ObjectSetInteger(0, levelName, OBJPROP_WIDTH, inpTriggerTHLevelWidth);
        ObjectSetDouble(0, levelName, OBJPROP_PRICE, priceAbove);
        
        drawnAbove++;
        priceAbove += stepSizePrice;
    }
    
    // Draw levels below midpoint
    int drawnBelow = 0;
    double priceBelow = midpointPrice - stepSizePrice;
    
    while(priceBelow >= g_lowestLow && drawnBelow < inpMaxTHLevelsBelow) {
        string levelName = objectPrefix + "CustomStep_Below_" + IntegerToString(drawnBelow + 1);
        
        if(ObjectFind(0, levelName) < 0) {
            ObjectCreate(0, levelName, OBJ_HLINE, 0, 0, priceBelow);
        }
        
        ObjectSetInteger(0, levelName, OBJPROP_COLOR, inpTriggerTHLevelColor);
        ObjectSetInteger(0, levelName, OBJPROP_STYLE, inpTriggerTHLevelStyle);
        ObjectSetInteger(0, levelName, OBJPROP_WIDTH, inpTriggerTHLevelWidth);
        ObjectSetDouble(0, levelName, OBJPROP_PRICE, priceBelow);
        
        drawnBelow++;
        priceBelow -= stepSizePrice;
    }
}
```

**استفاده در EventHandlers.mqh**:
```mql4
case E_STEP:
case TP_STEP:
{
    string prefix = objectPrefix + "TH_Level_";
    ObjectsDeleteAll(0, prefix);
    
    double eValue = CalculateEStep(thValue);
    double finalValue = (inpStepCalculationMode == TP_STEP || inpUseTPForEStep) ? 
                       CalculateTPStep(eValue) : eValue;
    
    // Convert to price units
    double finalValuePrice = finalValue * pointSize;
    
    // Draw with custom step size
    DrawCustomStepLevels(objectPrefix, midpointPrice, finalValuePrice);
    break;
}
```

---

**نتیجه**: اکثر موارد تکمیل شده اما E/TP و چند مورد دیگر نیاز به بررسی/اصلاح دارند.
