package com.biotak.util;

/**
 * کلاس ساده برای نمایش نحوه محاسبه دقیق تایم‌فریم‌های Pattern و Trigger
 * بدون نیاز به کامپایل با SDK موتیوویو
 */
public class ExactTimeframeCalculator {

    public static void main(String[] args) {
        System.out.println("=== محاسبه دقیق تایم‌فریم‌های Pattern و Trigger ===");
        System.out.println("این برنامه نشان می‌دهد که محاسبات دقیق ریاضی برای تایم‌فریم‌های غیر فراکتالی چگونه خواهد بود.\n");
        
        // تایم‌فریم‌های استاندارد
        testTimeframe(5, "M5");      // 5 دقیقه
        testTimeframe(15, "M15");    // 15 دقیقه
        testTimeframe(60, "H1");     // 1 ساعت
        
        System.out.println("\n=== تایم‌فریم‌های غیر فراکتالی ===\n");
        
        // تایم‌فریم‌های غیر فراکتالی که قبلاً مشکل داشتند
        testTimeframe(20, "M20");    // 20 دقیقه
        testTimeframe(45, "M45");    // 45 دقیقه
        testTimeframe(90, "H1+M30");  // 1.5 ساعت
        
        // موارد تست دیگر
        testTimeframe(17, "M17");    // 17 دقیقه (عدد اول)
        testTimeframe(23, "M23");    // 23 دقیقه (عدد اول دیگر)
        testTimeframe(120, "H2");    // 2 ساعت
        testTimeframe(180, "H3");    // 3 ساعت
    }
    
    private static void testTimeframe(int minutes, String timeframeName) {
        // محاسبه دقیق تایم‌فریم‌های الگو و اشاره‌گر
        int patternMinutes = Math.max(1, minutes / 4);
        int triggerMinutes = Math.max(1, minutes / 16);
        
        // محاسبه درصدها (به طور تقریبی برای نمایش)
        double timeframePercentage = getApproximatePercentage(minutes);
        double patternPercentage = getApproximatePercentage(patternMinutes);
        double triggerPercentage = getApproximatePercentage(triggerMinutes);
        
        // تبدیل به نام تایم‌فریم خوانا
        String patternName = formatTimeframe(patternMinutes);
        String triggerName = formatTimeframe(triggerMinutes);
        
        // نمایش اطلاعات دقیق
        System.out.println("تایم‌فریم: " + timeframeName + " (" + minutes + " دقیقه)");
        System.out.println("  درصد تایم‌فریم (تقریبی): " + String.format("%.5f%%", timeframePercentage));
        
        System.out.println("  تایم‌فریم الگو (Pattern): " + patternName + 
                           " (" + patternMinutes + " دقیقه = " + minutes + "/4)");
        System.out.println("    درصد تایم‌فریم الگو (تقریبی): " + String.format("%.5f%%", patternPercentage));
        
        System.out.println("  تایم‌فریم اشاره‌گر (Trigger): " + triggerName + 
                           " (" + triggerMinutes + " دقیقه = " + minutes + "/16)");
        System.out.println("    درصد تایم‌فریم اشاره‌گر (تقریبی): " + String.format("%.5f%%", triggerPercentage));
        
        // بررسی نسبت‌ها
        System.out.println("  بررسی محاسبات فراکتالی:");
        System.out.println("    - نسبت الگو به ساختار: " + String.format("%.2f", (double)patternMinutes / minutes) + 
                           " (انتظار: 0.25)");
        System.out.println("    - نسبت اشاره‌گر به ساختار: " + String.format("%.2f", (double)triggerMinutes / minutes) + 
                           " (انتظار: 0.0625)");
        System.out.println("    - نسبت درصدی الگو به ساختار: " + String.format("%.2f", patternPercentage / timeframePercentage) + 
                           " (انتظار: ~0.50)");
        System.out.println("    - نسبت درصدی اشاره‌گر به ساختار: " + String.format("%.2f", triggerPercentage / timeframePercentage) + 
                           " (انتظار: ~0.25)");
        
        System.out.println("----------------------------------------------------------");
    }
    
    /**
     * محاسبه تقریبی درصد تایم‌فریم بر اساس دقیقه
     * (تخمین ساده برای نمایش - در کد اصلی محاسبه دقیق‌تر است)
     */
    private static double getApproximatePercentage(int minutes) {
        if (minutes <= 1) return 0.02;  // M1
        if (minutes <= 5) return 0.04;  // M5
        if (minutes <= 15) return 0.08; // M15
        if (minutes <= 30) return 0.09; // M30
        if (minutes <= 60) return 0.12; // H1
        if (minutes <= 240) return 0.24; // H4
        if (minutes <= 1440) return 0.50; // D1
        return 0.76; // بیشتر از روزانه
    }
    
    /**
     * تبدیل تعداد دقایق به نام تایم‌فریم خوانا
     */
    private static String formatTimeframe(int minutes) {
        if (minutes < 60) {
            return "M" + minutes;
        } else if (minutes < 1440) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return "H" + hours;
            } else {
                return "H" + hours + "+M" + remainingMinutes;
            }
        } else {
            int days = minutes / 1440;
            int remainingMinutes = minutes % 1440;
            if (remainingMinutes == 0) {
                return "D" + days;
            } else {
                int hours = remainingMinutes / 60;
                int mins = remainingMinutes % 60;
                if (mins == 0) {
                    return "D" + days + "+H" + hours;
                } else {
                    return "D" + days + "+H" + hours + "+M" + mins;
                }
            }
        }
    }
} 