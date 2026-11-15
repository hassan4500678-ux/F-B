# FB ERP — Workforce & Attendance

Admin aur employees ke liye attendance app: check in / check out, present, absent, half day, graphs,
profile photo, attendance history. Targets, Reports, Salary waghera "Coming soon" modules hain.

## APK download

**https://github.com/hassan4500678-ux/F-B/releases/download/erp-latest/FB-ERP.apk**

Phone par link kholein, file download hogi, "Install" dabayein.

## App update

App khulte hi GitHub check karti hai. Naya version ho to Dashboard par **"Update available"** aata hai —
**Update now** dabayein, APK app ke andar download hogi aur install screen khul jayegi. Data waisa hi rehta hai.
Settings > App update se khud bhi check kar sakte hain. Pehli dafa phone "Allow from this source" ki ijazat maangega.

## Login

- Admin code: `4321` (Settings se badal sakte hain)
- Employees ka code admin "Add Employee" mein set karta hai.

## Rules

- Subah 9:15 ke baad check in = Half Day (admin Settings se time badal sakta hai).
- GPS: check in / check out sirf office ke 100 meter ke andar. Admin Settings > Office location mein office ki jagah save kare.
- Fake GPS apps pakdi jati hain.
- Sunday chutti hai, us din Absent nahi lagta.
- Saara data isi phone mein save hota hai.

## Badges aur Live status

- Admin sidebar > Badges se har employee ko badges de ya hata sakta hai. Employee ke dashboard par "My badges" mein dikhte hain.
- Admin dashboard par employee par tap karke edit ya delete karein.
- Dashboard aur sidebar mein Live office status: kaun Active (check in) hai aur kaun Offline.

## Files

- `web/index.html` — poori app (fonts andar hain, internet ke baghair chalti hai).
- `web/src.html` — app ka asal code. Badalne ke baad `cd ERP/web && python3 build.py index.html` chalayein.
- `android/` — Android app. `ERP/` mein koi bhi change `main` par push karein, GitHub nayi APK bana deta hai.
- `android/keystore/fb-erp.jks` — **kabhi delete ya change na karein**, warna nayi APK purani ke upar install nahi hogi.
