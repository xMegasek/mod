# Anarchia Bot (Fabric 1.21.4)

Mod kliencki: klawisz **K** otwiera panel, z którego dołączasz botem na serwer (domyślnie `anarchia.gg`)
i sterujesz nim: czat/komendy, klikanie slotów w GUI serwera, auto-akceptacja regulaminu, auto `/register` i `/login`.

## Jak to działa
Mod uruchamia w tle proces Node.js z biblioteką mineflayer (bot to osobne połączenie, Twój gracz zostaje tam, gdzie jest).
Pliki bota lądują w `.minecraft/config/anarchiabot/`.

## Wymagania
- Minecraft 1.21.4 + Fabric Loader + Fabric API
- Java 21
- **Node.js** (nodejs.org) — przy pierwszym "Dolacz" mod sam zrobi `npm install` (jest log w panelu)

## Budowanie
```
gradle wrapper        # raz, jeśli nie masz gradlew
./gradlew build       # (Windows: gradlew.bat build)
```
Gotowy plik: `build/libs/anarchia-bot-1.0.0.jar` → wrzuć do `.minecraft/mods/`.
Możesz też otworzyć folder w IntelliJ IDEA (Gradle się sam skonfiguruje).

## Użycie panelu
- **Nick / Serwer[:port] / Wersja / Haslo** – wersję zostaw pustą (auto). Hasło jest używane do automatycznego `/register` i `/login`.
- **Dolacz / Rozlacz** – start/stop bota.
- Pole na dole + **Wyslij** (lub Enter) – czat i komendy bota (np. `/login haslo`).
- **Pokaz GUI** – wypisuje sloty otwartego okna serwera; **Kliknij slot** – klika wskazany slot.
- **Auto-akceptacja regulaminu** – gdy otworzy się okno z itemem zawierającym np. „akceptuj”/„regulamin”, bot sam w niego kliknie.

## Uwagi
- Bot loguje się w trybie offline (nick bez konta Premium). Jeśli serwer wymaga konta Microsoft, trzeba to dopisać w `bot.js` (`auth: 'microsoft'`).
- Sprawdź regulamin serwera dotyczący botów/AFK, żeby nie dostać bana.
