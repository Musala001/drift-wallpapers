# Drift

An Android wallpaper app built around auto-rotation, using only public-domain art and wallpapers generated on the device.

## Features

- **Browse** public-domain paintings, prints and drawings from The Met and the Art Institute of Chicago by category or search, with the full credit line on every image.
- **Set** any image on the home screen, lock screen or both, cropped to your screen.
- **Rotate** automatically from your favourites, your own rotation list, a museum category or freshly generated designs. Shuffle avoids recent repeats; in-order walks the list. Choose 15 minutes to daily, Wi-Fi only, and it waits while the battery is low.
- **Live wallpaper** (optional): crossfades between rotated images, pans as you swipe home screens, double-tap for the next one.
- **Create** gradients, mesh blobs, waves, rings, stripes and halftone dots from colour-harmony palettes. Every design is reproducible from its id, so it can join your rotation list.
- **Next wallpaper** launcher shortcut (long-press the app icon).

## Setup

Requirements: JDK 17+ and the Android SDK (platform 37, build-tools). Point Gradle at the SDK with `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
```

On a machine with little RAM, cap Gradle in `~/.gradle/gradle.properties` (it overrides the project's `-Xmx4g`), for example:

```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.workers.max=1
kotlin.compiler.execution.strategy=in-process
```

`gradle.properties` sets `drift.githubUser`, which goes into the `AIC-User-Agent` header the Art Institute of Chicago asks API clients to send.

## Architecture

Single `:app` module, MVVM, Hilt for DI.

```
domain/       Pure Kotlin, unit-tested on the JVM
  model/      Artwork, Category, credit formatting
  image/      Portrait ranking, screen center-crop maths
  rotation/   RotationPlanner: in-order / shuffle-without-recent-repeats
  generator/  GeneratorSpec (id <-> style, harmony, seed), HSL palettes
data/
  remote/     Retrofit APIs, DTOs and public-domain mappers for each museum
    met/      The Met (v1.1 search + per-object lookups)
    aic/      Art Institute of Chicago (search via `params` JSON, IIIF image URLs)
    net/      Rate limiting, AIC header, API response caching, offline fallback
  local/      Room: artwork cache, Met negative cache, favourites, rotation list, history
  repository/ Merges museums into one feed; one museum failing doesn't fail the page
  rotation/   Settings (DataStore), RotationEngine, WorkManager scheduler and worker
  wallpaper/  Screen-sized bitmaps, WallpaperManager, live wallpaper hand-off
  generator/  Canvas renderer for generated designs
wallpaper/    DriftWallpaperService (live wallpaper)
di/           Hilt modules
ui/           Compose screens (Material 3)
```

### How rotation works

`RotationEngine.rotateNow()` reads the settings, lists candidate ids for the source (favourites, rotation list, a page of a museum category, or a new generated id), asks `RotationPlanner` for the next one, renders or downloads it at screen size and applies it. If Drift's live wallpaper is active, the image goes to the live wallpaper (a file plus a version flow) instead of replacing it with a still image. Each change is written to `rotation_history`, which the planner uses to avoid repeats.

The same engine runs from the Rotation screen's button, the periodic `RotationWorker`, the launcher shortcut (a one-time worker) and the live wallpaper's double-tap.

## Image sources and licensing

Drift only shows images that are free for commercial use.

| Source | Rule applied | Licence |
| --- | --- | --- |
| [The Metropolitan Museum of Art](https://metmuseum.github.io/) | `isPublicDomain == true` and a non-empty `primaryImage` | Open Access, CC0 |
| [Art Institute of Chicago](https://api.artic.edu/docs/) | `is_public_domain == true` and an `image_id` (filtered in the query and checked again in the app) | Public-domain images; API data CC0 |
| Generated in-app | Gradients and patterns drawn on the device | No licence needed |

Unsplash and Pexels are deliberately not used: their API terms don't allow wallpaper apps.

API etiquette: AIC requests carry `AIC-User-Agent` and are limited to 1/s (their anonymous limit is 60/min). Met requests are limited to 10/s (their limit is 80/s). API responses are cached for an hour, and the last cached copy is used when offline.

## Status

- [x] 1. Scaffold
- [x] 2. Data layer
- [x] 3. Browse and detail
- [x] 4. Rotation core
- [x] 5. Scheduled rotation (WorkManager)
- [x] 6. Live wallpaper
- [x] 7. Generator
- [x] 8. Polish
