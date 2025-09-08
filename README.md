# BorderPlugin

Deze plugin vergroot de wereldborder elke dag om 20:00 met een instelbaar aantal blokken. Instellingen zijn aanpasbaar via config.yml of het /borderreload commando.

## Installatie
1. Compileer de plugin tot een JAR-bestand en plaats deze in de plugins-map van je Paper server.
2. Start de server. De config.yml wordt automatisch aangemaakt.

## Configuratie
- `border.size`: Startgrootte van de border
- `border.center_x` en `border.center_z`: Coördinaten van het centrum
- `border.grow_amount`: Aantal blokken waarmee de border dagelijks groeit
- `border.grow_time`: Tijdstip waarop de border groeit (24u formaat, bv. 20:00)

## Commands
- `/borderreload` — Herlaad de config en pas direct de border aan

## Aanpassen
Wijzig de config.yml en gebruik `/borderreload` om wijzigingen toe te passen.
