# BiomeShop Android

Native Kotlin Android app based on [BiomeShop](https://biomeshop.github.io/home/).

## Download APK

- Stable APK link for your website: [Download latest APK](https://github.com/biomeshop/biomeapp/raw/main/downloads/biomeshop-debug.apk)
- Repository file page: [APK file in repo](https://github.com/biomeshop/biomeapp/blob/main/downloads/biomeshop-debug.apk)

## What it includes

- Kotlin + Jetpack Compose UI
- Hero section styled after the source site
- Featured biome section
- Filters for biome type, status, and price order
- Native biome inventory cards
- Remote image loading from the live Biomeshop asset host
- GitHub Actions workflow that builds a debug APK and uploads it as an artifact
- A checked-in APK file at `downloads/biomeshop-debug.apk` for a simple public website download link

## Why GitHub Actions is included

An APK does **not** require GitHub Actions in general. It can also be built locally with Android Studio or the Android SDK.

This repository includes GitHub Actions because the current machine where the project was prepared does not have Java or the Android SDK installed, so CI is the easiest way to produce the first APK artifact after pushing.

## Build in GitHub

Push the contents of this `android-app` folder to a GitHub repository.

Current simple public download flow:

1. Use the stable link above.
2. Download `biomeshop-debug.apk`.
3. Install it on Android.

If a newer APK is built later, replace `downloads/biomeshop-debug.apk` and push again to keep the website link pointing at the latest file.

## Local build

If you have Android Studio:

1. Open this folder as a project.
2. Let Gradle sync.
3. Run `assembleDebug` or build from Android Studio.
