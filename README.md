# Girls' Frontline Android Auto

![GitHub commit activity](https://img.shields.io/github/commit-activity/m/steve1316/gfl-android-auto?logo=GitHub) ![GitHub last commit](https://img.shields.io/github/last-commit/steve1316/gfl-android-auto?logo=GitHub) ![GitHub issues](https://img.shields.io/github/issues/steve1316/gfl-android-auto?logo=GitHub) ![GitHub pull requests](https://img.shields.io/github/issues-pr/steve1316/gfl-android-auto?logo=GitHub) ![GitHub](https://img.shields.io/github/license/steve1316/gfl-android-auto?logo=GitHub)

Project Summary TODO.

# Table of Contents

-   [Features](#Features)
-   [Requirements](#Requirements)
-   [Instructions](#Instructions)
-   [Technologies used](#Technologies-Used)

## Disclaimer

This application is developed with educational purposes in mind while exploring the potentials of automation and computer vision technologies.

# Features

-   [ ] Run a variety of maps to farm EXP and Cores.
    -   [x] 0-2
    -   [ ] 2-3
    -   [ ] 4-3e
    -   [ ] 5-2e
    -   [ ] 0-4
    -   [ ] 11-5
-   [x] Use specified Dummy and DPS Echelons.
    -   [ ] Swaps out DPS between Echelons for Corpse Dragging.
-   [x] ~~Enhance~~/Dismantle excess T-Dolls.
-   [x] Detect what T-Doll dropped as a reward during combat/operation end via Tesseract OCR.
-   [ ] Optional Discord integration for status and T-Doll alerts.
-   [ ] Optional integration with website (pending name and development) for statistical analysis like [Granblue Automation Statistics](https://granblue-automation-statistics.com/)

# Requirements

1. [Android Device (Nougat 7.0+)](https://developer.android.com/about/versions)

# Instructions

1. TODO

# Technologies Used

1. [MediaProjection - Used to obtain full screenshots](https://developer.android.com/reference/android/media/projection/MediaProjection)
2. [AccessibilityService - Used to dispatch gestures like tapping and scrolling](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
3. [OpenCV Android 4.5.1 - Used to template match](https://opencv.org/releases/)
4. [Tesseract4Android 2.1.1 - For performing OCR on the screen](https://github.com/adaptech-cz/Tesseract4Android)
5. [Google's Firebase Machine Learning OCR for Text Detection](https://developers.google.com/ml-kit)
6. [AppUpdater 2.7 - For automatically checking and notifying the user for new app updates](https://github.com/javiersantos/AppUpdater)
7. [Javacord 3.3.2 - For Discord integration](https://github.com/Javacord/Javacord)
8. [Klaxon 5.5 - For parsing JSON files](https://github.com/cbeust/klaxon)
9. [React Native 0.64.3 - Used to display the UI and manage bot settings](https://reactnative.dev/)
