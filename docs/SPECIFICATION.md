# Android Companion App Specification (v0.6.2)

## Objective

Build a native Android application (Kotlin + Jetpack Compose) that consumes the Hora Flask REST API. The app aims to provide a high-density, user-friendly mobile experience for accessing Vedic astrological data.

## Stack

-   Kotlin
-   Jetpack Compose (Material 3)
-   Jetpack Glance (Home Screen Widgets)
-   Retrofit + OkHttp (REST Networking)
-   Coil (SVG Image Loading)
-   WorkManager (Background Updates)
-   DataStore Preferences (Persistence)

## Backend API Support

The app consumes the following endpoints:
- `GET /api/v1/all`: Aggregate response for dashboard summary.
- `GET /api/v1/panchanga`: Detailed limbs with transition times.
- `GET /api/v1/hora`: Real-time and historical planetary hour data.
- `GET /api/v1/muhurta`: Calculated intervals (Rahu, Gulika, etc.).
- `GET /api/v1/dasha`: Vimshottari Dasha timeline and Balance.
- `GET /api/v1/kundali`: Detailed transit insights (Yogi, Karakas, Panchanga).
- `GET /api/v1/kundali/birth`: Detailed birth chart insights.
- `GET /api/v1/kundali/svg`: Real-time Transit Kundali Vector.
- `GET /api/v1/kundali/birth/svg`: Personalized Janma Kundali Vector.
- `POST /api/v1/matchmaking`: Guna Milan and compatibility analysis.
- `GET /api/v1/locations`: Saved location registry.
- `POST /api/v1/locations`: Custom location addition.
- `DELETE /api/v1/locations/{name}`: Custom location removal.

## Core Screens

1.  **Home**: Modern grid navigation providing a Hora countdown and Panchanga summary.
2.  **Panchanga Detail**: Full view of all limbs and calendar details with date navigation.
3.  **Hora Detail**: Current hora info with precise date/time selector support.
4.  **Solar & Celestial**: Human-readable solar events and Sun/Moon rasi positions.
5.  **Muhurta**: Timings for auspicious and inauspicious intervals.
6.  **Transit Kundali**: Multi-tab interface (**Info, Kundali, Dasha, Karakas**) with SVG visualization. 
    - **Info Tab**: Summary of Moon details, Active Dasha, Yogi/Avayogi/Duplicate Yogi planets, and Chara Karaka highlights (Atmakaraka & Darakaraka).
    - **Karakas Tab**: Detailed list of 7 Chara Karakas with planetary degrees and significations.
7.  **Birth Kundali**: Natal chart generation with integrated Dasha details and SVG rendering. Includes a centralized **Profile Manager** with encrypted local storage.
    - **Omni-Selection**: Integrated dialog to search, select, delete, or quickly add new profiles without leaving the screen.
    - **Smart Loading**: Loading a profile with only basic details (partial profile) automatically triggers a full API fetch to populate Dasha and Karaka data.
    - **Advanced Insights**: Same multi-tab structure as Transit, providing personalized Yogi and Karaka analysis.
8.  **Match Making**: Marriage compatibility tool using Guna Milan and Koota analysis.
    - **Dual Selection**: Choose Groom and Bride using the same centralized Profile Manager.
    - **Responsive Display**: Selection controls minimize to a compact top bar after results are loaded, maximizing space for compatibility details.
9.  **Locations**: Searchable registry with A-Z scrolling and multi-select deletion.
10. **Settings**: Application configuration (Language, API URL, Dasha depth, Session, and Kundali Save Path).

## Widgets
...
- **Widget Picker**: Enhanced selection experience with descriptive labels and screenshots.

## Advanced Astrological Features

### Yogi, Avayogi & Duplicate Yogi
- **Yogi Planet**: The prosperity-bestowing planet for the chart.
- **Avayogi Planet**: The planet representing obstacles or challenges.
- **Duplicate Yogi**: A secondary supportive planet.
- *Displayed in the "Info" tab of both Kundali screens.*

### Chara Karakas (Jaimini System)
- **Atmakaraka (AK)**: The "Soul" planet, representing the self and life purpose.
- **Amatyakaraka (AmK)**: Career and mental inclination.
- **Bhratrukaraka (BK)**: Siblings and mentors.
- **Matrukaraka (MK)**: Mother and domestic life.
- **Putrakaraka (PK)**: Children and creativity.
- **Gnatikaraka (GK)**: Relatives and competitors.
- **Darakaraka (DK)**: Spouse and partnerships.
- *Detailed in the dedicated "Karakas" tab with degrees, rasis, and significations.*
