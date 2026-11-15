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
- Friday chutti hai (attendance aur targets dono), us din Absent nahi lagta.
- Saara data isi phone mein save hota hai.

## Badges aur Live status

- Admin sidebar > Badges se har employee ko badges de ya hata sakta hai. Employee ke dashboard par "My badges" mein dikhte hain.
- Admin dashboard par employee par tap karke edit ya delete karein.
- Dashboard aur sidebar mein Live office status: kaun Active (check in) hai aur kaun Offline.

## Targets (CTN)

- **New Targets** (admin): employee, mahina aur saal chun kar CTN target assign karein. Foran employee ke Targets mein dikhta hai.
- **Targets**: monthly target, achieved, remaining, live daily target (zyada bechne par agle din ka target kam, kam bechne par zyada),
  time gone vs achievement, graphs (bars, line, pie), daily log aur pichle mahine se comparison.
- Target mahine ke working days (Friday chhod kar, jaise November = 26) se divide hota hai.
- Achievement Daily Form ke CTN se khud judti hai. Admin "Manual adjustment" se extra CTN daal sakta hai.
- Admin ko sab ka total: target, achieved, per day, live daily aur aaj ka achievement.
- Har mahine ki aakhri tareekh raat **9 baje** woh mahina lock ho jata hai — target aur achievement mein koi change nahi.
- Month filter se purane mahine dekhein; admin "All employees" ya aik aik employee dekh sakta hai.

## Daily Form

- Employee Profile > My areas mein areas ke naam daale: Core 3, Vab 6, Ws Core 3.
- Daily Form: area chunein, total shops 45 (khud), productive shops aur CTN daal kar Save.
  Neeche aaj ki daily summary. CTN foran target achievement mein jud jate hain (employee aur admin dono ke paas).

## Analytics (sirf admin)

Month aur booker filter. Targets vs achievement, target split, productive vs non-productive, area-wise shops,
booker-wise productivity, daily trend, saare bookers ki detail table aur area detail table.

## Performance modules

- **Target Predictor**: pichle 7 working days ki raftaar se mahine ke aakhir ka andaza, 100% ke liye rozana kitna chahiye, at-risk bookers.
- **Area Heatmap**: map par areas hare/peele/laal (productivity). Area ki jagah Profile > My areas ke pin se ya Daily Form save karte waqt GPS se.
- **Leaderboard**: Achievement %, CTN, Productivity, Attendance, Overall — podium aur poori ranking.
- **Comparison**: mahina vs mahina (team ya booker), booker vs booker.
- **Reports**: daily / monthly report, WhatsApp par share ya copy.
- **Report Card**: mahine ka card (attendance, target %, productivity, badges, grade A+ se D) — image share/save.
  Grade = 50% target + 25% attendance + 25% productivity. A+ 90+, A 80+, B 70+, C 60+, D neeche.

## Mazbooti (long term)

- Koi screen error de to app band nahi hoti; dashboard khul jata hai aur error andar note ho jata hai.
- Purane data ko naye version mein khud update (migrate) kiya jata hai.
- Settings > Backup & restore: mahine mein kam az kam aik dafa backup WhatsApp/Drive par rakhein.
- Android 5 se Android 16 tak (Android System WebView updated ho).

## Kaun kya dekhta hai

| Module | Admin | Employee |
|---|---|---|
| Target Predictor | Sab | — |
| Leaderboard | Poori ranking | Poori ranking + apni position |
| Area Heatmap | Sab areas | Apne areas |
| Report Card | Sab | Apna |
| Comparison | Team / booker | Apna mahina vs mahina |

## Files

- `web/index.html` — poori app (fonts andar hain, internet ke baghair chalti hai).
- `web/src.html` — app ka asal code. Badalne ke baad `cd ERP/web && python3 build.py index.html` chalayein.
- `android/` — Android app. `ERP/` mein koi bhi change `main` par push karein, GitHub nayi APK bana deta hai.
- `android/keystore/fb-erp.jks` — **kabhi delete ya change na karein**, warna nayi APK purani ke upar install nahi hogi.
