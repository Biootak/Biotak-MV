---
inclusion: always
---

# راهنمای توسعه MotiveWave - برای کدنویسی سریع و بهینه

## منابع اصلی

### 1. مستندات رسمی MotiveWave SDK
- **URL**: https://www.motivewave.com/sdk/javadoc7/index.html
- **نسخه**: 7.0 (آخرین نسخه)
- **کاربرد**: مرجع کامل API ها، کلاس‌ها و متدهای SDK

### 2. کتابخانه mwave_sdk.jar
#[[file:lib/mwave_sdk.jar]]
- **مسیر**: `lib/mwave_sdk.jar`
- **نسخه**: 7.0
- **شامل**: تمام کلاس‌های SDK برای توسعه اندیکاتورها

### 3. نمونه کدهای داخلی پروژه
#[[file:src/study_examples]]
- **مسیر**: `src/study_examples/`
- **شامل**: نمونه‌های عملی اندیکاتورهای مختلف
- **فایل‌های کلیدی**:
  - `MyMovingAverage.java` - نمونه میانگین متحرک
  - `SampleMACross.java` - نمونه تقاطع میانگین‌ها با سیگنال
  - `SimpleMACD.java` - نمونه MACD ساده
  - `ATRChannel.java` - نمونه کانال ATR
  - `TrendLine.java` - نمونه خط روند

## الگوهای کدنویسی MotiveWave

### 1. ساختار کلی Study
```java
@StudyHeader(
    namespace="com.mycompany", 
    id="UNIQUE_ID",
    name="Study Name",
    desc="Study Description",
    menu="Menu Category",
    overlay=true/false,  // overlay روی چارت اصلی یا پنجره جداگانه
    signals=true/false   // آیا سیگنال تولید می‌کند
)
public class MyStudy extends Study {
    enum Values { VALUE1, VALUE2 };  // مقادیر محاسبه شده
    enum Signals { SIGNAL1, SIGNAL2 }; // سیگنال‌ها (اختیاری)
    
    @Override
    public void initialize(Defaults defaults) {
        // تنظیمات کاربر و تعریف runtime
    }
    
    @Override
    protected void calculate(int index, DataContext ctx) {
        // محاسبات اصلی
    }
}
```

### 2. تنظیمات کاربر (Settings)
```java
@Override
public void initialize(Defaults defaults) {
    var sd = createSD();
    var tab = sd.addTab("General");
    var grp = tab.addGroup("Inputs");
    
    // انواع مختلف Input ها:
    grp.addRow(new InputDescriptor(Inputs.INPUT, "Input", Enums.BarInput.CLOSE));
    grp.addRow(new IntegerDescriptor(Inputs.PERIOD, "Period", 20, 1, 9999, 1));
    grp.addRow(new DoubleDescriptor(Inputs.MULTIPLIER, "Multiplier", 2.0, 0.1, 10.0, 0.1));
    grp.addRow(new MAMethodDescriptor(Inputs.METHOD, "Method", Enums.MAMethod.EMA));
    grp.addRow(new PathDescriptor(Inputs.PATH, "Path", defaults.getLineColor(), 1.0f, null, true, false, false));
    grp.addRow(new MarkerDescriptor(Inputs.UP_MARKER, "Up Marker", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getLineColor(), true, true));
}
```

### 3. Runtime Descriptor
```java
var desc = createRD();
desc.setLabelSettings(Inputs.INPUT, Inputs.PERIOD); // برای نمایش در label
desc.exportValue(new ValueDescriptor(Values.MA, "Moving Average", new String[] {Inputs.INPUT, Inputs.PERIOD}));
desc.declarePath(Values.MA, Inputs.PATH); // برای رسم خط
desc.declareBars(Values.HIST, Inputs.BAR); // برای رسم histogram
desc.declareSignal(Signals.CROSS_ABOVE, "Cross Above Signal"); // برای سیگنال
desc.setRangeKeys(Values.MA); // برای تعیین محدوده نمایش
```

### 4. محاسبات در calculate()
```java
@Override
protected void calculate(int index, DataContext ctx) {
    int period = getSettings().getInteger(Inputs.PERIOD);
    if (index < period) return; // داده کافی نیست
    
    var series = ctx.getDataSeries();
    Object input = getSettings().getInput(Inputs.INPUT);
    
    // محاسبه میانگین متحرک
    Double ma = series.ema(index, period, input);
    if (ma == null) return;
    
    // ذخیره مقدار
    series.setDouble(index, Values.MA, ma);
    
    // بررسی تکمیل بار (برای سیگنال‌ها)
    if (!series.isBarComplete(index)) return;
    
    // تولید سیگنال
    if (crossedAbove(series, index, Values.FAST_MA, Values.SLOW_MA)) {
        ctx.signal(index, Signals.CROSS_ABOVE, "Fast MA Crossed Above!", series.getClose(index));
    }
    
    series.setComplete(index);
}
```

## متدهای مفید DataSeries

### محاسبات آماری
```java
// میانگین‌های متحرک
Double sma = series.sma(index, period, input);
Double ema = series.ema(index, period, input);
Double wma = series.wma(index, period, input);
Double ma = series.ma(method, index, period, input);

// محاسبات آماری
Double highest = series.highest(index, period, input);
Double lowest = series.lowest(index, period, input);
Double stdDev = series.stdDev(index, period, input);
Double variance = series.variance(index, period, input);

// اندیکاتورهای تکنیکال
Double rsi = series.rsi(index, period, input);
Double atr = series.atr(index, period);
Double adx = series.adx(index, period);
```

### دسترسی به داده‌ها
```java
// قیمت‌های OHLC
double open = series.getOpen(index);
double high = series.getHigh(index);
double low = series.getLow(index);
double close = series.getClose(index);
long volume = series.getVolume(index);

// مقادیر محاسبه شده قبلی
Double prevValue = series.getDouble(index-1, Values.MA);

// بررسی تکمیل بار
boolean isComplete = series.isBarComplete(index);
```

## بهترین روش‌های کدنویسی

### 1. Performance Optimization
```java
// استفاده از cache برای محاسبات پیچیده
private final Map<Integer, Double> cache = new HashMap<>();

// بررسی null قبل از استفاده
if (value == null) return;

// استفاده از primitive types در جای مناسب
int period = getSettings().getInteger(Inputs.PERIOD);
double multiplier = getSettings().getDouble(Inputs.MULTIPLIER);
```

### 2. Error Handling
```java
@Override
protected void calculate(int index, DataContext ctx) {
    try {
        // محاسبات اصلی
    } catch (Exception e) {
        error("Error in calculation at index " + index + ": " + e.getMessage());
        return;
    }
}
```

### 3. Debug و Logging
```java
// استفاده از debug برای troubleshooting
debug("Calculating for index: " + index + ", value: " + value);

// استفاده از info برای اطلاعات مهم
info("Study initialized with period: " + period);

// استفاده از warn برای هشدارها
warn("Insufficient data at index: " + index);
```

### 4. Memory Management
```java
// پاک کردن cache در صورت نیاز
@Override
public void onBarUpdate(DataContext ctx) {
    cache.clear(); // پاک کردن cache در بروزرسانی
}

// استفاده از WeakReference برای object های بزرگ
private WeakReference<LargeObject> largeObjectRef;
```

## انواع مختلف Study

### 1. Overlay Studies (روی چارت اصلی)
```java
@StudyHeader(overlay=true)
// مثال: Moving Average, Bollinger Bands, Support/Resistance
```

### 2. Panel Studies (پنجره جداگانه)
```java
@StudyHeader(overlay=false)
// مثال: RSI, MACD, Stochastic
```

### 3. Signal Studies (تولید سیگنال)
```java
@StudyHeader(signals=true)
// استفاده از ctx.signal() برای تولید سیگنال
```

## نکات مهم برای JDK 24 و MotiveWave 7

### 1. استفاده از ویژگی‌های جدید Java
```java
// Records برای data classes
public record PriceData(double open, double high, double low, double close) {}

// Switch expressions
var result = switch (method) {
    case SMA -> series.sma(index, period, input);
    case EMA -> series.ema(index, period, input);
    case WMA -> series.wma(index, period, input);
    default -> throw new IllegalArgumentException("Unknown method: " + method);
};

// Text blocks برای strings طولانی
var description = """
    This indicator calculates the moving average
    using the specified method and period.
    """;
```

### 2. Pattern Matching
```java
// استفاده از pattern matching در instanceof
if (obj instanceof Double d && d > 0) {
    // استفاده از d
}
```

## منابع اضافی و به‌روزرسانی

### GitHub Repositories (1-3 سال اخیر)
- جستجو برای "motivewave study" در GitHub
- بررسی repositories با activity اخیر
- مطالعه implementation های مختلف

### Community Resources
- MotiveWave Forums
- Trading View Pine Script (برای ایده‌های الگوریتمی)
- QuantConnect و سایر پلتفرم‌های algorithmic trading

### Best Practices از منابع بروز
- استفاده از modern Java features
- Performance optimization techniques
- Memory management در real-time applications
- Multi-threading considerations

## چک‌لیست توسعه سریع

### قبل از شروع کدنویسی:
- [ ] تعیین نوع study (overlay/panel/signal)
- [ ] مشخص کردن inputs مورد نیاز
- [ ] طراحی Values enum
- [ ] تعیین نحوه نمایش (path/bars/markers)

### حین کدنویسی:
- [ ] استفاده از الگوهای موجود در study_examples
- [ ] بررسی null values
- [ ] اضافه کردن debug logs
- [ ] تست با داده‌های مختلف

### بعد از کدنویسی:
- [ ] تست performance با داده‌های زیاد
- [ ] بررسی memory usage
- [ ] تست در شرایط مختلف بازار
- [ ] مستندسازی کد

این راهنما به شما کمک می‌کند تا سریع‌تر و بهتر کد MotiveWave بنویسید با استفاده از تمام منابع موجود.