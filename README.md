# BiomeShop Android

Native Kotlin Android app based on [BiomeShop](https://biomeshop.github.io/home/).

## Download APK

- Stable APK link for your website: [Download latest APK](https://github.com/biomeshop/biomeapp/releases/download/apk-latest/biomeshop-debug.apk)
- GitHub release page: [Latest APK release](https://github.com/biomeshop/biomeapp/releases/tag/apk-latest)

## What it includes

- Kotlin + Jetpack Compose UI
- Hero section styled after the source site
- Featured biome section
- Filters for biome type, status, and price order
- Native biome inventory cards
- Remote image loading from the live Biomeshop asset host
- GitHub Actions workflow that builds a debug APK, uploads it as an artifact, and publishes a stable release download link on `main`

## Why GitHub Actions is included

An APK does **not** require GitHub Actions in general. It can also be built locally with Android Studio or the Android SDK.

This repository includes GitHub Actions because the current machine where the project was prepared does not have Java or the Android SDK installed, so CI is the easiest way to produce the first APK artifact after pushing.

## Build in GitHub

Push the contents of this `android-app` folder to a GitHub repository.

After the workflow runs on `main`:

1. Use the stable link above, or open the `apk-latest` release page.
2. Download `biomeshop-debug.apk`.
3. Install it on Android.

On non-`main` branches, the workflow still uploads the APK as an Actions artifact for testing.

## Local build

If you have Android Studio:

1. Open this folder as a project.
2. Let Gradle sync.
3. Run `assembleDebug` or build from Android Studio.
