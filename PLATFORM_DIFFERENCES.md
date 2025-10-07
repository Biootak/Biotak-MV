# تفاوت‌های معماری MT4 و MotiveWave - طراحی عمدی

تاریخ: 2025-10-07

---

## 🎯 تفاوت‌های عمدی و مفید

برخی تفاوت‌ها بین MT4 و MotiveWave **عمدی** و **مفید** هستند و نباید یکسان شوند.

---

## 1️⃣ محاسبه Historical High/Low

### MotiveWave (Java):
```java
// Java approach: Automatic optimization
int maxBarsToScan = Math.min(sz, 1000); // محدودیت خودکار 1000 کندل
```

**دلیل**:
- MotiveWave برای performance از محدودیت خودکار استفاده می‌کند
- کاربر نمی‌تواند این را تغییر دهد
- برای اکثر موارد 1000 کندل کافی است

### MT4 (MQL4):
```mql4
// MT4 approach: User control
int totalBars = inpHistoricalPeriods == 0 ? 
               iBars(Symbol(), inpHistoricalTimeframe) : 
               inpHistoricalPeriods;
```

**دلیل**:
- ✅ کاربر کنترل کامل دارد
- ✅ می‌تواند از timeframe دلخواه استفاده کند (D1, W1, MN1, etc.)
- ✅ می‌تواند تعداد کندل‌ها را دستی تنظیم کند
- ✅ انعطاف‌پذیری بیشتر برای استراتژی‌های مختلف

---

## 2️⃣ چرا این تفاوت خوب است؟

### مزایای رویکرد MT4:

1. **کنترل بیشتر**:
   ```
   inpHistoricalPeriods = 0        → همه کندل‌های موجود
   inpHistoricalPeriods = 100      → فقط 100 کندل اخیر
   inpHistoricalPeriods = 5000     → 5000 کندل (برای تحلیل بلندمدت)
   ```

2. **انتخاب Timeframe**:
   ```
   inpHistoricalTimeframe = PERIOD_D1   → High/Low روزانه
   inpHistoricalTimeframe = PERIOD_W1   → High/Low هفتگی
   inpHistoricalTimeframe = PERIOD_MN1  → High/Low ماهانه
   ```

3. **سازگاری با استراتژی**:
   - Day Trading: کندل‌های کمتر نیاز است
   - Swing Trading: کندل‌های متوسط
   - Position Trading: کندل‌های زیاد برای دید بلندمدت

### مزایای رویکرد Java:

1. **سادگی**: کاربر نیازی به تنظیم ندارد
2. **Performance**: محدودیت خودکار از مشکلات جلوگیری می‌کند
3. **یکپارچگی**: همه کاربران تجربه یکسان دارند

---

## 3️⃣ نتیجه‌گیری

این تفاوت **نباید** یکسان شود چون:

1. ✅ **MT4**: پلتفرمی برای تریدرهای حرفه‌ای که به کنترل نیاز دارند
2. ✅ **MotiveWave**: پلتفرمی برای سادگی و یکپارچگی

هر دو رویکرد **صحیح** هستند اما برای مخاطبان مختلف.

---

## 4️⃣ تفاوت‌های دیگر قابل قبول

### A. Cache Strategy
- **Java**: کش پیچیده با expiry time
- **MT4**: کش ساده‌تر با static variables
- **نتیجه**: هر دو مناسب پلتفرم خودشان

### B. Error Handling
- **Java**: Exception handling جامع
- **MT4**: Print statements و return codes
- **نتیجه**: هر دو مطابق convention پلتفرم خود

### C. Settings Management
- **Java**: Settings object با persistence
- **MT4**: Input parameters + GlobalVariables
- **نتیجه**: هر دو استاندارد پلتفرم خودشان

---

## 5️⃣ موارد باید یکسان باشند ✅

اینها **باید** یکسان باشند (و هستند):

1. ✅ فرمول‌های محاسباتی (TH, Fractal Values, SS/LS)
2. ✅ منطق رسم سطوح (shouldDrawStep, price limits)
3. ✅ تبدیل واحدها (points ↔ price units)
4. ✅ Timeframe percentages
5. ✅ فرمول‌های E, TP, M

---

## 📊 خلاصه مقایسه

| ویژگی | Java | MT4 | باید یکسان باشد؟ |
|-------|------|-----|-----------------|
| فرمول‌های TH | ✅ | ✅ | ✅ بله |
| Fractal Values | ✅ | ✅ | ✅ بله |
| SS/LS/M/E/TP | ✅ | ✅ | ✅ بله |
| منطق رسم | ✅ | ✅ | ✅ بله |
| **Historical Range** | Auto (1000) | User Control | ❌ خیر - هر کدام مناسب |
| Cache Strategy | Complex | Simple | ❌ خیر - هر کدام مناسب |
| Settings | Object | Inputs | ❌ خیر - هر کدام مناسب |

---

## 🎯 نتیجه نهایی

**محاسبات و نتایج یکسان هستند، اما راه رسیدن به آن‌ها می‌تواند متفاوت باشد.**

این تفاوت‌ها نشان‌دهنده **طراحی هوشمندانه** برای هر پلتفرم است، نه اشکال!

---

**سخت‌افزار مثال**:
- دو اتومبیل با موتور یکسان اما کنترل‌های مختلف (دستی vs اتوماتیک)
- هر دو به مقصد می‌رسند، اما تجربه رانندگی متفاوت است
- هیچ‌کدام "اشتباه" نیست، فقط برای کاربران مختلف طراحی شده‌اند
