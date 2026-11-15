# FB ERP — Google Play setup (Internal testing)

Internal testing par app foran employees tak pahunchti hai: Play Store se install, **koi Play Protect warning nahi**,
updates Play Store khud deta hai. Isme 100 tak log ho sakte hain aur **12 testers / 14 din wali shart nahi** lagti
(woh sirf public "Production" ke liye hai).

## Kya download karna hai

GitHub Releases > **erp-latest** se:

- `FB-ERP-play.aab` — Play Console mein upload karne wali file
- Is folder se: `icon-512.png`, `feature-graphic-1024x500.png`, aur `screenshot-*.png`

## Steps (Play Console mein, aap khud)

1. **Create app**
   - App name: `FB ERP`
   - Default language: English (United States)
   - App or game: **App** · Free or paid: **Free**
   - Declarations tick karein → Create app

2. **Testing → Internal testing → Create new release**
   - "Play App Signing" ko **Continue / Accept** karein (Google signing sambhalega)
   - `FB-ERP-play.aab` upload karein
   - Release name khud aa jayega (1.x) → **Next → Save → Rollout**

3. **Testers** tab
   - "Create email list" → naam `FB Team` → har employee ki **Gmail** daalein → Save
   - "Copy link" — yehi **opt-in link** employees ko WhatsApp par bhejein

4. **App content** (left menu → Policy → App content) — sab bharna zaroori hai:
   - **Privacy policy:** `https://github.com/hassan4500678-ux/F-B/blob/main/ERP/PRIVACY.md`
   - **Ads:** No, my app does not contain ads
   - **App access:** "All or some functionality is restricted" → instructions:
     `Login code for review: 4321 (administrator).`
   - **Content rating:** questionnaire → Category **Utility/Productivity**, sab sawalon par **No** → rating mil jayegi
   - **Target audience:** 18 and over
   - **Data safety:** neeche dekhein
   - **Government app:** No · **Financial features:** None · **Health:** No
   - **Location permission:** foreground only (background nahi)

5. **Store listing** (Grow → Store presence → Main store listing)
   - Short description:
     `Attendance, targets, daily forms and analytics for the FB sales team.`
   - Full description:
     ```
     FB ERP is the internal workforce app for the Faizan Brothers field team.

     • GPS check in / check out within 100 m of the office
     • Monthly CTN targets with live daily target and predictor
     • Daily form: areas, shops, productive shops and cartons
     • Analytics, area heatmap, leaderboard and comparison
     • Monthly report card with grade, shareable on WhatsApp
     • Badges, attendance history and backup / restore

     For Faizan Brothers staff only. Sign in with the code given by your administrator.
     ```
   - App icon: `icon-512.png` · Feature graphic: `feature-graphic-1024x500.png`
   - Phone screenshots: `screenshot-01` se `screenshot-07` tak (kam az kam 2)
   - App category: **Business** · Email: aapki email

## Data safety answers

| Question | Answer |
|---|---|
| Does your app collect or share any required user data types? | **No** — all data stays on the device |
| Is all data encrypted in transit? | Yes (updates and maps use HTTPS) |
| Account creation / deletion | Users cannot create accounts; admin deletes employees inside the app |

(Agar baad mein online database lagaya to yeh jawab badalne honge.)

## Employees ke liye

1. Admin ka bheja hua **opt-in link** kholein (wahi Gmail jo list mein hai, Play Store mein bhi wahi)
2. **Become a tester** dabayein → "Download it on Google Play" → **Install**
3. Agar pehle GitHub wali APK lagi hai to **pehle usko uninstall** karein (dono ka signature alag hai)

## Updates

Har nayi GitHub build ke sath `FB-ERP-play.aab` banti hai. Internal testing → Create new release → nayi `.aab` upload →
Rollout. Employees ke phone par Play Store khud update kar deta hai.

Isko bhi automatic karna ho (har change par Play Console mein khud upload), to Play Console mein aik
"service account" banana hoga — yeh baad mein kar sakte hain.
