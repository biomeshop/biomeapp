# BiomeShop Android

Native Kotlin Android app based on [BiomeShop](https://biomeshop.github.io/home/).

## What it includes

- Kotlin + Jetpack Compose UI
- Hero section styled after the source site
- Featured biome section
- Filters for biome type, status, and price order
- Native biome inventory cards
- Remote image loading from the live Biomeshop asset host
- GitHub Actions workflow that builds a debug APK and uploads it as an artifact

## Why GitHub Actions is included

An APK does **not** require GitHub Actions in general. It can also be built locally with Android Studio or the Android SDK.

This repository includes GitHub Actions because the current machine where the project was prepared does not have Java or the Android SDK installed, so CI is the easiest way to produce the first APK artifact after pushing.

## Build in GitHub

Push the contents of this `android-app` folder to a GitHub repository.

After the workflow runs:

1. Open the repository's `Actions` tab.
2. Open the latest `Build Android APK` run.
3. Download the `biomeshop-debug-apk` artifact.
4. Extract it and install the `app-debug.apk` file on Android.

## Local build

If you have Android Studio:

1. Open this folder as a project.
2. Let Gradle sync.
3. Run `assembleDebug` or build from Android Studio.
