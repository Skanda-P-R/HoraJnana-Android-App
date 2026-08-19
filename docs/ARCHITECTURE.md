# Architecture Overview - HoraJnana

This document describes the high-level design and package structure of the HoraJnana Android app.

## Project Structure

The app follows a standard Android Clean Architecture approach with a focus on simplicity and native Jetpack Compose patterns.

- `api/`: Contains the Retrofit interface (`HoraApiService`) for consuming the Flask REST backend.
- `repository/`: The `HoraRepository` handles all data fetching and coordination between the network and the local cache (`CacheManager`).
- `models/`: Kotlin data classes representing the backend JSON responses.
- `ui/`:
    - `screens/`:
        - `HomeScreen`: Central grid navigation.
        - `PanchangaDetailScreen`: Detailed limbs with date control.
        - `HoraDetailScreen`: Instant/historical Hora data.
        - `SolarCelestialScreen`: Human-friendly celestial events.
        - `MuhurtaScreen`: Interval timing dashboard.
        - `TransitKundaliScreen`: Real-time chart visualization.
        - `BirthKundaliScreen`: Janma Kundali generation form.
        - `MatchMakingScreen`: Guna Milan compatibility tool.
        - `LocationsScreen`: Advanced registry with A-Z index and multi-select.
        - `SettingsScreen`: App configuration.
    - `navigation/`: (Integrated in `MainActivity`) Logic for switching between screens.
- `widgets/`: Implementation of Home Screen widgets using Jetpack Glance.
- `workers/`: `WorkManager` tasks for background data synchronization.
- `utils/`: 
    - `TranslationUtils`: UI label translations.
    - `WidgetUtils`: Helpers for triggering widget refreshes.
    - `EncryptionUtils`: AES-based encryption for local file protection.

## Core Design Principles

### 1. Robust Data Layer
The repository is designed to be "Offline-First" where possible.
- **Caching**: Every successful JSON fetch is saved to internal storage.
- **Vault System**: Personalized Birth Kundalis can be explicitly saved to the local file system. These files are encrypted at rest using `EncryptionUtils` to prevent unauthorized access by other document readers.
- **Vault Browser**: Discovers and indexes saved kundalis by pre-fetching lightweight metadata (Name, DOB, POB) for instant searching and sorting without loading full datasets into memory.
- **Performance Optimization**: Uses a centralized, shared `Moshi` instance and lazy ViewModel initialization to minimize reflection overhead and ensure near-instant app startup.
- **Error Handling**: If a network fetch fails, the app returns the last cached version along with a timestamp and the error message to the UI.

### 2. Localization (Kannada Support)
Since astrological content is often preferred in regional languages, the app includes a comprehensive translation layer:
- **Server-Side Translation**: Primary data values (Limbs, Planets, Rasis) are translated by the backend via the `lang` parameter.
- **UI Translation**: Static UI labels (Headings, Buttons, etc.) are handled by `TranslationUtils` in the app.
- **Language Persistence**: User language choice is saved in `DataStore` and shared between the app UI and the widgets.

### 3. Background Synchronization
- **WorkManager**: A periodic worker runs every 15 minutes to refresh data and keep widgets accurate.
- **Immediate Sync**: Manual refreshes in the app dashboard trigger an immediate update to the Home Screen widgets using `WidgetUtils`.

## Testing Strategy
- **Unit Tests**: Planned for `TranslationUtils` and `HoraRepository` parsing logic.
- **UI Tests**: Compose-based tests for screen navigation.
