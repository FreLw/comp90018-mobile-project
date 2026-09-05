Lost Treasures of UniMelb

This is the COMP90018 mobile project prototype.

Current scope:

- Kotlin + Jetpack Compose front-end only
- Sample data only
- No real GPS, sensor, cloud, login, or notification integration yet

Code structure:

```text
app/src/main/java/com/unimelb/losttreasures/
  MainActivity.kt                App entry point
  LostTreasuresPreview.kt        Compose preview entry

  ui/
    LostTreasuresApp.kt          Bottom navigation and screen switching
    theme/Theme.kt               Shared colors and Material theme
    model/                       UI data models
    data/SampleData.kt           Temporary sample data for previews
    components/                  Reusable UI components
    screens/                     Profile, Square, Map, Collection, and Team screens
```

Preview:

Open `LostTreasuresPreview.kt` or `MainActivity.kt`, then use Android Studio's Compose Preview.

Future integration:

- Replace `ui/data/SampleData.kt` with ViewModel or repository data.
- Keep screen files mostly stateless by passing data and callbacks into them.
- Add tests around model transformation and ViewModel state before wiring real sensors and cloud services.
