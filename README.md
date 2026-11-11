# FB Claims — Faizan & Brothers Claim Management App

Yeh Android app wahi **Faizan & Brothers VIP Claim Management** page kholti hai jo claude.ai par bana hai.
Design, reports aur data bilkul same hain: app mein jo claim save karein woh website par dikhta hai,
aur website par jo save ho woh app mein. Data live chalta hai.

## APK download

Hamesha latest APK is link se milti hai:

**https://github.com/hassan4500678-ux/F-B/releases/latest/download/FB-Claims.apk**

Phone par link kholein, file download hogi, phir "Install" dabayein
(pehli dafa "Unknown apps / is source se install" allow karna padega).

## Pehli dafa

1. App kholein. Claims page Chrome ke andar khulega.
2. claude.ai par apne account se login karein (sirf ek dafa).
3. Bas — ab har dafa app seedha claims par khulegi.

## App ke buttons

| Button | Kaam |
|---|---|
| Claims App kholein | Live claims system |
| Taaza karein | Claims page ka latest version (naye changes khud aa jate hain) |
| Naya app update install karein | Jab GitHub par nayi APK bane to yeh button aata hai. Dabane se update download ho kar install screen khulti hai |
| WhatsApp par PDF report bhejein | Download ki hui PDF chunein, seedha WhatsApp par jayegi |
| Downloads / PDF files dekhein | Phone ke downloads |

## Updates kaise aate hain

- **Claims page ke changes** (jo Claude se karwayein): app mein khud aa jate hain, nayi APK ki zaroorat nahi.
- **App ke changes**: `main` branch par push hote hi GitHub Actions nayi APK banata hai aur Release mein daal deta hai.
  Phone par app khud bata deti hai ke naya update aaya hai.

## Zaroori

- `ClaimsApp/keystore/fb-claims.jks` **kabhi delete ya change na karein**. Isi key ki wajah se har nayi APK purani ke upar
  install hoti hai.
- Data claude.ai par save hota hai, phone mein nahi — app uninstall karne se bhi data nahi jata.
- Google Chrome phone mein hona chahiye.
