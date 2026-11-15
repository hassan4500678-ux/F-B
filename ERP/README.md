# FB ERP — Workforce & Attendance

Admin aur employees ke liye attendance app: check in / check out, present, absent, half day, graphs,
profile photo, attendance history. Targets, Reports, Salary waghera "Coming soon" modules hain.

## APK download

**https://github.com/hassan4500678-ux/F-B/releases/download/erp-latest/FB-ERP.apk**

Phone par link kholein, file download hogi, "Install" dabayein.

## Login

- Admin code: `4321` (Settings se badal sakte hain)
- Employees ka code admin "Add Employee" mein set karta hai.

## Rules

- Check out par 4 ghante se kam = Half Day.
- Sunday chutti hai, us din Absent nahi lagta.
- Saara data isi phone mein save hota hai.

## Files

- `web/index.html` — poori app (fonts andar hain, internet ke baghair chalti hai).
- `web/src.html` — app ka asal code. Badalne ke baad `cd ERP/web && python3 build.py index.html` chalayein.
- `android/` — Android app. `ERP/` mein koi bhi change `main` par push karein, GitHub nayi APK bana deta hai.
- `android/keystore/fb-erp.jks` — **kabhi delete ya change na karein**, warna nayi APK purani ke upar install nahi hogi.
