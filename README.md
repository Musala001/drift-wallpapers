# Drift

An Android wallpaper app built around auto-rotation, using only public-domain art and wallpapers generated on the device.

> Work in progress. Built in phases; see [Status](#status).

## Setup

Requirements: JDK 17+ and the Android SDK (platform 37, build-tools). Point Gradle at the SDK with `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

`gradle.properties` sets `drift.githubUser`, which goes into the `AIC-User-Agent` header the Art Institute of Chicago asks API clients to send.

## Architecture

Single `:app` module, MVVM, Hilt for DI.

```
domain/      Pure Kotlin: Artwork, Category, credit formatting, portrait ranking
data/
  remote/    Retrofit APIs, DTOs and public-domain mappers for each museum
    met/     The Met (v1.1 search + per-object lookups)
    aic/     Art Institute of Chicago (search via `params` JSON, IIIF image URLs)
    net/     Rate limiting, AIC header, API response caching, offline fallback
  local/     Room: artwork cache, Met negative cache, favourites
  repository/ Merges museums into one feed; one museum failing doesn't fail the page
di/          Hilt modules
ui/          Compose screens (Material 3)
```

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
- [ ] 8. Polish
