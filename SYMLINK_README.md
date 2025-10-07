# 🔗 راه‌اندازی Symbolic Link برای MT4

## چرا Symbolic Link؟

با Symbolic Link:
- ✅ **هیچ کپی کردنی نیست!** فایل‌ها در یک جا هستند
- ✅ **تغییرات فوری** - بلافاصله در MT4 دیده می‌شود
- ✅ **بدون اشتباه** - دیگر فراموش نمی‌کنید فایل را کپی کنید
- ✅ **راه حل ویندوزی** - بدون نیاز به اسکریپت اضافی

## 🚀 نصب (یکبار اجرا کنید)

### گام 1: بستن MT4
اگر MT4 باز است، آن را ببندید.

### گام 2: اجرای فایل
**روی `create_symlink.bat` راست کلیک کنید** و **"Run as administrator"** را انتخاب کنید.

### گام 3: تایید
اگر ویندوز پیغام UAC نشان داد، **"Yes"** را بزنید.

### گام 4: انجام شد! ✅

اگر پیغام **SUCCESS** را دیدید، همه چیز آماده است!

## 🎯 نحوه استفاده

بعد از نصب:

1. **فایل‌ها را در پروژه ویرایش کنید**
   ```
   C:\Users\Fatemehkh\IdeaProject\Biotak\Mt4\Biotak Trigger TH3\
   ```

2. **ذخیره کنید** (Ctrl+S)

3. **بلافاصله** در MT4 قابل مشاهده است:
   ```
   C:\Users\...\MQL4\Indicators\Biotak Trigger TH3\
   ```

**هیچ کپی کردنی لازم نیست!** 🎉

## 🧪 تست

برای اطمینان:

1. فایلی مثل `PropertiesAndInputs.mqh` را باز کنید
2. یک comment اضافه کنید و ذخیره کنید
3. همان فایل را در MT4 MetaEditor باز کنید
4. باید comment جدید را ببینید!

## 🔧 عیب‌یابی

### مشکل: "You do not have sufficient privilege"
- فایل را با **"Run as administrator"** اجرا کنید (راست کلیک)

### مشکل: "Source folder not found"
- مطمئن شوید مسیر پروژه درست است:
  ```
  C:\Users\Fatemehkh\IdeaProject\Biotak\Mt4\Biotak Trigger TH3
  ```

### مشکل: "Could not remove existing target"
- MT4 را ببندید
- دوباره اسکریپت را اجرا کنید

### مشکل: Symlink کار نمی‌کند
- در Explorer بروید به:
  ```
  C:\Users\Fatemehkh\AppData\Roaming\MetaQuotes\Terminal\0727F3F88B5F0FE006962B330B91FF37\MQL4\Indicators\
  ```
- باید پوشه "Biotak Trigger TH3" با آیکون فلش (→) باشد

## 📋 چک لیست

- [ ] MT4 را بستم
- [ ] `create_symlink.bat` را با **Run as administrator** اجرا کردم
- [ ] پیغام **SUCCESS** را دیدم
- [ ] تست کردم و کار می‌کند

## ℹ️ نکات مهم

1. **Symlink فقط یکبار** نیاز به نصب دارد
2. **بعد از نصب** می‌توانید مستقیماً در پروژه کار کنید
3. **Git** فقط فایل‌های پروژه را track می‌کند (نه MT4)
4. **حذف symlink**: فقط پوشه در MT4 را delete کنید (فایل‌های اصلی safe هستند)

## 🎬 Workflow جدید

```
قبل:
Edit → Save → Copy to MT4 → Compile → Test
      ^^^^^^^^^^^^^^^^^^^^^ (اضافی!)

بعد:
Edit → Save → Compile → Test
      ^^^^^ (فقط همین!)
```

---

**نکته**: این روش استاندارد ویندوز است و توسط برنامه‌نویسان حرفه‌ای استفاده می‌شود! 🎯
