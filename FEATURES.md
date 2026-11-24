# Plant Keeper: features en stack

## Doel

Android-app (Samsung en andere Android-toestellen) om lokaal bij te houden:
- welke zaden je hebt,
- welke planten je hebt gezaaid,
- wanneer je wat kunt zaaien.

Geen account, geen cloud. Alle data staat op het toestel. Backup en herstel via export en import.

## Stack

- **Taal en UI**: Kotlin, Jetpack Compose, Material 3
- **Data**: Room (SQLite), lokaal
- **Instellingen**: DataStore
- **Foto's**: één foto per item, automatisch verkleind naar maximaal 1024 px, opgeslagen in de interne app-map. Het pad staat in de database.
- **Herinneringen**: WorkManager (dagelijkse controle) plus notificaties. Op Android 13+ vraagt de app om de POST_NOTIFICATIONS-permissie.
- **Backup**: kotlinx.serialization (JSON) in een ZIP samen met de foto's, via de bestandskiezer (Storage Access Framework). Geen opslagpermissies nodig.
- **minSdk**: 26 (Android 8). **targetSdk**: nieuwste.
- **Tests**: JUnit voor de zaaikalender-logica en backup, Room in-memory voor de DAO's.

## MVP-features

### 1. Zaden

- Velden: naam, soort, hoeveelheid, houdbaar tot (jaar), bewaarlocatie, notities, foto (optioneel)
- Zaad bewerken en verwijderen, verwijderen met bevestiging
- Verlopen zaden (houdbaar tot-jaar voorbij) krijgen een opvallend label in de lijst
- Hoeveelheid is een **getal plus eenheid**. Eenheden om uit te kiezen: zakje, bus, bollen, gram, stuks. Bijvoorbeeld 2 zakjes of 4 bussen. Het is een schatting, exact tellen is niet nodig.
- Geen automatische aftrek van de voorraad bij zaaien. De hoeveelheid is informatief en pas je zelf aan.
- Verwijderen van een zaad: de bijbehorende planten blijven bestaan, de koppeling wordt leeg. De plant houdt zijn eigen naam en zaaidatum.
- Zaaitijden per zaad, apart voor **binnen** en **buiten**, als begin- en eindmaand (1 t/m 12)
- Een zaaiperiode mag over de jaarwissel lopen (bijvoorbeeld oktober t/m februari)
- Bewaarlocatie in vrije tekst (bijvoorbeeld "bus 3, kast")
- Zoeken op naam
- **Zaadgewinning**: een geoogste plant kan een nieuw zaad-record maken, gekoppeld aan de moederplant (zaadoogstdatum, hoeveelheid als schatting)

### 2. Zaaikalender

- Overzicht "nu zaaibaar": alle zaden waarvan de zaaiperiode de huidige maand bevat, gesplitst op binnen en buiten
- Dit is het startscherm van de app
- Planten staan niet in de kalender, alleen in hun eigen lijst

### 3. Planten (gezaaid)

- Koppelen aan een zaad (of een losse plant met vrije naam)
- Een zaad kan **meerdere keren** gezaaid worden (bijvoorbeeld binnen in februari en buiten in mei). Elke zaai is een eigen plant-record met een eigen zaaidatum en status.
- Velden: zaaidatum, locatie (vrije tekst, bijvoorbeeld "vensterbank" of "bak 2"), notities, foto (optioneel)
- Status: gezaaid, ontkiemd, uitgeplant, geoogst, mislukt, overleden. Overleden planten blijven in de geschiedenis staan.
- Status vrij kiezen, zonder verplichte volgorde
- Oogst simpel: een oogstdatum en een vrij tekstveld voor hoeveelheid en notitie
- Filteren op status
- Plant bewerken en verwijderen, verwijderen met bevestiging

### 4. Herinneringen

- Per zaad aan of uit te zetten
- Een melding op de eerste dag van de startmaand van een zaaiperiode. Start binnen en buiten in dezelfde maand, dan is er toch maar **één** melding per zaad.
- Een dagelijkse WorkManager-controle bepaalt of er een melding moet komen, zodat er geen exacte alarmen nodig zijn

### 5. Backup

- **Exporteren**: één ZIP met `data.json` en de map `photos/`
- **Importeren**: kies een ZIP, daarna de keuze:
  - *Samenvoegen*: items met een bestaand id worden bijgewerkt, nieuwe items worden toegevoegd
  - *Vervangen*: de huidige data wordt gewist, na bevestiging
- Het JSON-bestand bevat een schemaversie, zodat latere wijzigingen gemigreerd kunnen worden
- Alle records krijgen een UUID als id, geen oplopend nummer. Alleen zo werkt samenvoegen op id tussen toestellen.
- De app waarschuwt dat data verloren gaat als de app wordt verwijderd zonder eerst te exporteren
- Geen versleuteling in MVP. De ZIP is leesbaar, dus bewaar hem zorgvuldig. Versleuteling met wachtwoord kan later.

## Buiten scope (MVP)

- Accounts, cloudsync, multi-device sync
- iOS
- AI-plantherkenning
- Kiemkracht en kiemtesten (gezaaid versus ontkiemd, percentages)
- Takenlijst en verzorgingstaken (water geven, verpotten, oogsten), ook herhalingen
- Verzorgingslog (water, bemesten)
- Oogstlog met jaaroverzicht (bijvoorbeeld 20 kg aardappelen), later uit te werken als cool feature
- Check-in meldingen per plant ("hoe gaat het met...")
- Widgets (nice to have, zie hieronder)
- Meertalig (alleen Nederlands)

## Nice to have (later, nog niet uitgewerkt)

- **Zaaiplanner**: een planning die bijhoudt wat je wanneer gaat zaaien. Idee staat, uitwerking later.
- **Widget** op het startscherm van Android. Haalbaarheid nog uitzoeken (Jetpack Glance is de Compose-manier voor widgets).

## Beslissingen (vastgelegd)

- Naam: Plant Keeper
- Taal: alleen Nederlands
- Geen aankoopdatum en geen leverancier bij zaden
- Hoeveelheid: getal plus eenheid (zakje, bus, bollen, gram, stuks)
- Overleden is een plantstatus, naast verwijderen
- Oogstlog en check-in meldingen staan buiten de MVP
- Startscherm: nu zaaibaar. Status vrij kiezen. Oogst simpel. Bewaarlocatie in vrije tekst.
- Thema: lichte en donkere modus volgen het systeem
- Herinneringen: één melding per zaaiperiode, op de eerste dag van de startmaand
- Zaadgewinning zit in MVP. Zaaitijden zijn vaste maanden. Locaties zijn vrije tekst. Taken staan buiten de MVP.
- Backup: geen versleuteling in MVP
- Foto's: één per item, verkleind naar maximaal 1024 px
