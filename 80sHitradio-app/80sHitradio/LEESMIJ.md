# 80s Hitradio – Android-app

## 1. Stream
De stream staat al ingesteld: `https://s22.myradiostream.com:7728/listen.mp3`
(aan te passen in `app/src/main/res/values/strings.xml`).

## 2. APK maken
1. Installeer Android Studio (gratis).
2. File → Open → kies deze map. Wacht tot "Gradle sync" klaar is.
3. Telefoon via USB + ontwikkelaarsopties aan → groene ▶ Run-knop,
   óf Build → Build App Bundle(s) / APK(s) → Build APK(s).

## Wat zit erin
- Live speler die doorspeelt op de achtergrond, met Stop-knop in de melding
- "Nu op de radio" en "Gespeelde nummers" (dezelfde widgets als op de website), elke minuut ververst
- Geen extra codec-bibliotheken: Android speelt MP3/AAC zelf af

## Logo
Het logo zit in de app (`res/drawable/logo.png`) en is ook het app-icoon (`logo_icon.png`).
Ander logo? Vervang die twee bestanden door nieuwe met dezelfde naam.

## APK maken zonder Android Studio (via GitHub, gratis)
1. Maak een gratis account op github.com → "New repository".
2. "uploading an existing file" → sleep alle bestanden en mappen uit deze map erin → Commit.
3. Tabblad "Actions" → "Bouw APK" draait vanzelf (± 5 min).
4. Klik op de groene run → onderaan "80sHitradio-apk" downloaden → uitpakken → APK op je telefoon zetten.
