/**
 * تست دستی محاسبات ATR برای تأیید صحت
 */
public class TestATR {
    
    public static void main(String[] args) {
        // نمونه داده برای تست
        // کندل 1: High=1.2050, Low=1.2020, Close=1.2035
        // کندل 2: High=1.2045, Low=1.2015, Close=1.2025, PrevClose=1.2035
        // کندل 3: High=1.2055, Low=1.2025, Close=1.2040, PrevClose=1.2025
        
        System.out.println("=== تست محاسبه True Range ===");
        
        // کندل 2
        double high2 = 1.2045;
        double low2 = 1.2015;
        double prevClose2 = 1.2035;
        
        double hl2 = high2 - low2;                           // 1.2045 - 1.2015 = 0.0030
        double hc2 = Math.abs(high2 - prevClose2);           // |1.2045 - 1.2035| = 0.0010
        double lc2 = Math.abs(low2 - prevClose2);            // |1.2015 - 1.2035| = 0.0020
        
        double tr2 = Math.max(hl2, Math.max(hc2, lc2));     // max(0.0030, 0.0010, 0.0020) = 0.0030
        
        System.out.printf("کندل 2: HL=%.4f, HC=%.4f, LC=%.4f, TR=%.4f%n", hl2, hc2, lc2, tr2);
        
        // کندل 3
        double high3 = 1.2055;
        double low3 = 1.2025;
        double prevClose3 = 1.2025;
        
        double hl3 = high3 - low3;                           // 1.2055 - 1.2025 = 0.0030
        double hc3 = Math.abs(high3 - prevClose3);           // |1.2055 - 1.2025| = 0.0030
        double lc3 = Math.abs(low3 - prevClose3);            // |1.2025 - 1.2025| = 0.0000
        
        double tr3 = Math.max(hl3, Math.max(hc3, lc3));     // max(0.0030, 0.0030, 0.0000) = 0.0030
        
        System.out.printf("کندل 3: HL=%.4f, HC=%.4f, LC=%.4f, TR=%.4f%n", hl3, hc3, lc3, tr3);
        
        // محاسبه ATR دو دوره‌ای
        double atr2Period = (tr2 + tr3) / 2;                 // (0.0030 + 0.0030) / 2 = 0.0030
        
        System.out.printf("ATR (2 دوره): %.4f%n", atr2Period);
        
        System.out.println("\n=== مقایسه با الگوریتم کد ===");
        
        // شبیه‌سازی الگوریتم کد
        double tr2_code = calculateTR_CodeStyle(high2, low2, prevClose2);
        double tr3_code = calculateTR_CodeStyle(high3, low3, prevClose3);
        double atr_code = (tr2_code + tr3_code) / 2;
        
        System.out.printf("TR2 (کد): %.4f, TR3 (کد): %.4f, ATR (کد): %.4f%n", tr2_code, tr3_code, atr_code);
        
        // بررسی تطابق
        boolean isCorrect = Math.abs(atr2Period - atr_code) < 0.00001;
        System.out.println("نتیجه: " + (isCorrect ? "✅ محاسبات درست است" : "❌ خطا در محاسبات"));
    }
    
    // شبیه‌سازی روش محاسبه در کد اصلی
    private static double calculateTR_CodeStyle(double high, double low, double prevClose) {
        double hl = high - low;
        double hc = Math.abs(high - prevClose);
        double lc = Math.abs(low - prevClose);
        
        double tr = hl;
        if (hc > tr) tr = hc;
        if (lc > tr) tr = lc;
        
        return tr;
    }
}
