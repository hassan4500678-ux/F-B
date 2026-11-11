# FB Claims — Faizan & Brothers Claim Management App

Faizan & Brothers ka VIP Claim Management system, ek mukammal Android app ki shakal mein.
App kholte hi seedha chalti hai — koi link, login ya loading nahi. Internet ke baghair bhi chalti hai.

## APK download

Hamesha latest APK:

**https://github.com/hassan4500678-ux/F-B/releases/latest/download/FB-Claims.apk**

Phone par link kholein, file download hogi, "Install" dabayein.

## Data

- Saara data isi phone mein, app ke andar save hota hai. App band karne, phone restart karne ya
  app update karne se data nahi jata.
- **Settings > Data Backup > Backup file banayein** se har hafte backup lein aur WhatsApp ya Google Drive par rakhein.
- Naye phone par app install karke **Backup restore karein** se saare claims wapas aa jate hain.
- App uninstall ya "Clear data" karne se phone ka data mit jata hai — is liye backup zaroori hai.

## PDF aur WhatsApp

"Report / Export PDF" ya claim slip dabane par PDF phone ke **Downloads / FB Claims** folder mein save hoti hai,
aur usi waqt "WhatsApp par bhejein" ka button aata hai.

## Updates

- App ka page `web/claims.html` mein hai (Claude artifact ki exact copy). Isay badal kar `main` par push karein —
  GitHub Actions nayi APK bana kar Release mein daal deta hai.
- Phone par app internet milte hi khud batati hai "Naya update aaya hai". Download karke install karein; data waise hi rehta hai.

## Zaroori

- `ClaimsApp/keystore/fb-claims.jks` **kabhi delete ya change na karein**. Isi key ki wajah se har nayi APK
  purani ke upar install hoti hai aur data bacha rehta hai.
