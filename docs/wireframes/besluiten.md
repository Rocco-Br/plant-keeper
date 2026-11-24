# Wireframe-besluiten

Datum: 2026-11-24. Bron: interview met de gebruiker over de UI. Alles wat al in `FEATURES.md` staat (blok "Beslissingen") blijft gelden.

## Vastgelegd in het interview

| Onderwerp | Besluit |
|---|---|
| Navigatie | Bottom bar met 4 tabs: Nu zaaibaar, Zaden, Planten, Meer. Onder Meer staan Herinneringen en Backup. |
| Startscherm | Een scroll-lijst met de kop "Binnen" en daaronder "Buiten". Elk zaad toont naam, hoeveelheid en een label "verlopen" waar van toepassing. |
| Lijsten zaden en planten | Lijst met een kleine thumbnail, naam en een regel met details. Geen grid. |
| Formulieren zaad en plant | Volledig scherm, met "Opslaan" rechtsboven. Geen bottom sheet, geen dialoog. |
| Zaad-detail | Zaaitijden binnen en buiten (met jaarbalk), lijst van gekoppelde planten, schakelaar voor de herinnering, knop "Zaaien". |
| Foto's | Op detailschermen een grote foto bovenin. In het formulier een knop "Foto kiezen" met preview. Zonder foto een grijze placeholder. |
| Backup | Eén scherm met "Exporteren" en "Importeren". Bij importeren de keuze tussen samenvoegen en vervangen, met bevestiging. Een waarschuwing dat data verloren gaat zonder export. |
| Stijl | Rustig, natuurlijk groen. Lichte en donkere modus volgen het systeem. |

## Voorstel van de wireframe-sessie (bevestigd door de gebruiker)

- **Plant-detail**: foto, zaaidatum, status (vrij te kiezen), locatie, link naar het gekoppelde zaad (of "losse plant"), notities, oogstsectie (datum, hoeveelheid, notitie), knop "Maak zaad van deze plant" voor zaadgewinning.
- **Plant-formulier**: zaad kiezen of losse plant met vrije naam, zaaidatum, locatie, status (standaard "gezaaid"), notities, foto.
- **Zaad-formulier**: naam, soort, hoeveelheid (getal plus eenheid), houdbaar tot (jaar), bewaarlocatie, zaaitijd binnen en buiten (begin- en eindmaand), notities, foto. De herinnering-schakelaar staat alleen op het detailscherm.
- **Herinneringen**: bovenin uitleg (melding op de eerste dag van de startmaand), een melding als de permissie ontbreekt, en per zaad een schakelaar.
- **Meer**: een menu met twee regels, Herinneringen en Backup.
- **Zoeken**: zoekbalk bovenin de zaden- en plantenlijst.
- **Verwijderen**: via een bevestigingsdialoog. Die staat niet als eigen scherm in de wireframes.

## Nog open

- Exacte kleuren en huisstijl (komt bij het Compose-thema, niet in de wireframes).
- Tabnamen en iconen definitief maken.
- Widget en zaaiplanner blijven buiten de MVP.
