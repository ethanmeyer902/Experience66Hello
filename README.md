# Experience 66 - Route 66 Arizona Explorer

An Android application showcasing historic Route 66 landmarks in Arizona with geofencing and offline capabilities.

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Mapbox](https://img.shields.io/badge/Mapbox-000000?style=flat&logo=mapbox&logoColor=white)

## Features

### C3 – Geofencing Landmark Detection
Automatically detects when users approach historic Route 66 landmarks in Arizona.

| Feature | Description |
|---------|-------------|
| **11 Arizona Landmarks** | Oatman, Kingman, Hackberry, Seligman, Williams, Flagstaff, Meteor Crater, Winslow, Jack Rabbit, Holbrook, Petrified Forest |
| **Geofence Events** | Enter, Exit, and Dwell detection |
| **Visual Feedback** | Red markers for landmarks, blue circles for geofence radius |
| **Monitor Panel** | Real-time event log with timestamps |

### C1 – Offline Caching for Maps & CONTENTdm Data
Supports offline access for areas with limited connectivity along Route 66.

| Feature | Description |
|---------|-------------|
| **Network Monitoring** | Real-time online/offline status indicator |
| **Landmark Cache** | Stores metadata, historical notes, and coordinates |
| **Offline Maps** | Downloads map tiles for Arizona Route 66 corridor |
| **Auto-Cache** | Automatically caches data on first launch |

## Screenshots

```
┌─────────────────────────────────┐
│ 🟢 Online          [📥 Cache]  │  ← Status Bar
├─────────────────────────────────┤
│                                 │
│     [Map with Route 66]         │
│        📍 Flagstaff             │
│     ◯ Geofence circles          │
│        📍 Winslow               │
│                                 │
├─────────────────────────────────┤
│ [Toggle Monitor]                │
│ ┌─────────────────────────────┐ │
│ │ REGISTERED GEOFENCES: 11    │ │
│ │ • Oatman Ghost Town (300m)  │ │
│ │ • Kingman (500m)            │ │
│ │ ...                         │ │
│ ├─────────────────────────────┤ │
│ │ EVENT LOG:                  │ │
│ │ 14:32 ENTER Flagstaff       │ │
│ │ 14:28 EXIT Williams         │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Arizona Route 66 Landmarks

| # | Landmark | Location | Geofence Radius |
|---|----------|----------|-----------------|
| 1 | Oatman Ghost Town | 35.0264, -114.3823 | 300m |
| 2 | Kingman | 35.1894, -114.0530 | 500m |
| 3 | Hackberry General Store | 35.3707, -113.7301 | 200m |
| 4 | Seligman | 35.3274, -112.8767 | 400m |
| 5 | Williams | 35.2494, -112.1910 | 500m |
| 6 | Flagstaff | 35.1983, -111.6513 | 600m |
| 7 | Meteor Crater | 35.0275, -111.0225 | 400m |
| 8 | Standin' on the Corner (Winslow) | 35.0242, -110.6974 | 150m |
| 9 | Jack Rabbit Trading Post | 35.0245, -110.1042 | 200m |
| 10 | Wigwam Motel (Holbrook) | 34.9014, -110.1580 | 200m |
| 11 | Petrified Forest National Park | 35.0657, -109.7890 | 1000m |

## Requirements

- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0)
- Mapbox Account (for access token)

## Setup

### 1. Clone the Repository
```bash
git clone https://github.com/ethanmeyer902/Experience66Hello.git
cd Experience66
```

### 2. Mapbox Configuration

**Public Token** (for map display):
- Get your token from [Mapbox Account](https://account.mapbox.com/)
- Add to `app/src/main/res/values/mapbox-resource-token.xml`:
```xml
<string name="mapbox_access_token">YOUR_PUBLIC_TOKEN</string>
```

**Secret Token** (for SDK download):
- Create a secret token with `Downloads:Read` scope
- Add to `gradle.properties`:
```properties
MAPBOX_DOWNLOADS_TOKEN=sk.your_secret_token_here
```

### 3. Build & Run
```bash
./gradlew assembleDebug
```

Or open in Android Studio and run on device/emulator.

## Project Structure

```
Experience66/
├── app/
│   ├── src/main/java/com/example/experience66hello/
│   │   ├── MainActivity.kt              # Main app with map and UI
│   │   ├── Route66Landmark.kt           # Landmark data model
│   │   ├── GeofenceManager.kt           # Geofence registration
│   │   ├── GeofenceBroadcastReceiver.kt # Geofence event handling
│   │   ├── OfflineMapManager.kt         # Offline map tiles
│   │   ├── OfflineDataCache.kt          # Landmark metadata cache
│   │   ├── LandmarkDataCache.kt         # Historical data cache
│   │   └── NetworkUtils.kt              # Network monitoring
│   └── src/main/res/
│       ├── drawable/red_marker.xml      # Landmark marker icon
│       └── values/mapbox-resource-token.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Precise location for geofencing |
| `ACCESS_BACKGROUND_LOCATION` | Geofence detection when app is backgrounded |
| `INTERNET` | Map tiles and online features |
| `ACCESS_NETWORK_STATE` | Offline mode detection |

## Demo Instructions

### Testing C3 - Geofencing
1. Launch the app and grant location permissions
2. Tap "Toggle Monitor" to see registered geofences
3. Use Android Studio's emulator location controls to simulate movement
4. Set location to a landmark (e.g., Flagstaff: 35.1983, -111.6513)
5. Observe "ENTER" event in the monitor panel

### Testing C1 - Offline Caching
1. Launch the app (auto-caches data if online)
2. Observe green "🟢 Online" status bar
3. Tap "📥 Cache" to manually download offline data
4. Enable Airplane Mode on device
5. Observe orange "📴 Offline" status bar
6. App continues working with cached data

## Technologies

- **Kotlin** - Primary language
- **Mapbox Maps SDK 11.5** - Interactive maps
- **Google Play Services Location 21.2** - Geofencing API
- **Android Jetpack** - Activity, Lifecycle components
- **SharedPreferences** - Local data caching

## License

This project is for educational purposes as part of the Experience 66 Route 66 preservation initiative.

---

*"Get your kicks on Route 66"* 🛣️

