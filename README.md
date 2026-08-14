# HoraJnana Android (Beta v0.8.4)

A native Android application providing real-time astrological (Panchanga) and Hora data by consuming a dedicated Flask REST API. Designed with high-density Home Screen widgets and full Kannada language support.

## Key Features

- **Modern Grid Navigation**: A clean, 2-column square matrix on the Home screen for quick access to all astrological tools.
- **Real-time Hora**: Tracks the current astrological hour with remaining time, upcoming Hora planet, and historical/future lookups.
- **Full Panchanga Detail**: Comprehensive view of Samvatsara, Ayana, Rutu, Masa, Paksha, Tithi, Nakshatra, Yoga, Karana, and Vara with exact end times.
- **Solar & Celestial Insights**: Human-readable data for Sunrise, Sunset, Solar Noon, durations, and Sun/Moon Rasi positions.
- **Muhurta Timings**: Dedicated view for Rahu Kalam, Gulika, Yamaganda, and Abhijit Muhurta for any selected date.
- **Birth & Transit Kundali (SVG)**: View real-time transit charts or generate Janma Kundali (Birth Charts) with perfectly sharp SVG vector rendering.
- **Secure Kundali Vault**: Save generated Birth Kundalis locally with encrypted JSON storage. Load saved charts instantly for offline viewing.
- **Intelligent Place of Birth Search**: Integrated searchable birth location dropdown with pre-fetched API data for zero-latency selection.
- **Vimshottari Dasha Explorer**: Integrated multi-tab interface in Kundali screens to view Moon details, Dasha balance, and a hierarchical 3-level Dasha timeline (Mahadasha, Antardasha, Pratyantardasha).
- **Advanced Location Registry**: Switch between automatic GPS tracking and a searchable manual location database with A-Z indexing and custom entry support.
- **High-Density Widgets**: Android Home Screen widgets built with Jetpack Glance, featuring a realistic selection picker and curved modern previews.
- **Multilingual Support**: Full English and Kannada support for all UI labels and backend-driven data values.
- **Modern Authentication**: Secure Google Sign-In for seamless access using your existing Google account.

## Security & Authentication

Introduced in v0.6.0, the app implements a robust identity and authorization layer:
- **Google Sign-In**: Leverages Jetpack Credential Manager for a secure, one-tap login experience using your Google account.
- **ID Token Verification**: The app sends a cryptographically signed Google ID Token to the backend, which is verified using Google's public keys.
- **Bearer Token Auth**: All subsequent API communication is secured using a session token (JWT/Bearer) with automatic expiration and invalidation.
- **Local Data Encryption**: Saved Kundali files are encrypted using AES, ensuring that sensitive birth data remains unreadable by other applications.
- **Privacy Guard**: Backup is disabled to ensure data is wiped on uninstall, and network logging is silenced in production builds.

## Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Widgets**: Jetpack Glance
- **Networking**: Retrofit + OkHttp (with custom Interceptors) + Moshi (Kotlin Reflection)
- **Async**: Kotlin Coroutines & Flow
- **Authentication**: Jetpack Credential Manager + Google Identity
- **Security**: Android Keystore & Encrypted Storage
- **Persistence**: DataStore (Preferences) & Internal Storage
- **Location**: Google Play Services (Fused Location Provider)
- **Image Loading**: Coil

## Quick Start

1. **Clone the repository** and open the project in Android Studio (Ladybug or newer recommended).
2. **Sync Gradle** and ensure you have Android SDK 35 installed.
3. **Run the app** and sign in with your Google account.
4. **Grant Location Permissions** to allow the app to fetch local astrological data.

## Documentation

For more detailed information, please refer to the following documents:

- [**Security & Authorization**](docs/SECURITY.md): Details on the Google Sign-In integration and backend whitelisting architecture.
- [**Legal Notices**](docs/LEGAL_NOTICES.md): Acknowledgments for Swiss Ephemeris and licensing details.
- [**Architecture Overview**](docs/ARCHITECTURE.md): Technical details on the app's structure and design patterns.
- [**Widget Guide**](docs/WIDGETS.md): Instructions on adding and configuring Home Screen widgets.
- [**Original Specification**](docs/SPECIFICATION.md): The initial project requirements and goals.

## Licensing

This project is licensed under the **GNU Affero General Public License v3 (AGPL-3.0)**. See the [LICENSE](LICENSE) file for the full license text.

This application consumes the [HoraJnana REST API](https://github.com/Skanda-P-R/HoraJnana-REST-API.git), which utilizes the **Swiss Ephemeris** for high-precision astronomical calculations. In compliance with the Swiss Ephemeris public license, this project is also released under the AGPL.

*Swiss Ephemeris is copyright © Astrodienst AG, Switzerland.*

---
*Built for the Hora API ecosystem.*
