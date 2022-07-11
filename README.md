# Girls' Frontline Android Auto

![GitHub commit activity](https://img.shields.io/github/commit-activity/m/steve1316/gfl-android-auto?logo=GitHub) ![GitHub last commit](https://img.shields.io/github/last-commit/steve1316/gfl-android-auto?logo=GitHub) ![GitHub issues](https://img.shields.io/github/issues/steve1316/gfl-android-auto?logo=GitHub) ![GitHub pull requests](https://img.shields.io/github/issues-pr/steve1316/gfl-android-auto?logo=GitHub) ![GitHub](https://img.shields.io/github/license/steve1316/gfl-android-auto?logo=GitHub)

This project aims to provide a basic Android-only implementation of automation for the popular mobile game, Girls' Frontline. A similar, more sophisticated bot is located at [WAI2K](https://github.com/waicool20/WAI2K).

# Table of Contents

-   [Features](#Features)
-   [Requirements](#Requirements)
-   [Instructions](#Instructions)
-   [Technologies used](#Technologies-Used)

## Disclaimer

This application is developed with educational purposes in mind while exploring the potentials of automation and computer vision technologies.

# Features

-   [x] Run a variety of maps to farm EXP and Cores.
    -   [x] 0-2
    -   [x] 2-3
    -   [x] 4-3e
    -   [x] 5-2e
    -   [x] 0-4
    -   [x] 11-5
-   [x] Use specified Dummy and DPS Echelons.
    -   [x] Swaps out DPS between Echelons for Corpse Dragging.
-   [x] ~~Enhance~~/Dismantle excess T-Dolls.
-   [x] Detect what T-Doll dropped as a reward during combat/operation end via Tesseract OCR.
-   [ ] Optional Discord integration for status and T-Doll alerts.
-   [ ] Optional integration with website (pending name and development) for statistical analysis like [Granblue Automation Statistics](https://granblue-automation-statistics.com/)

# Requirements

1. [Android Device (Nougat 7.0+)](https://developer.android.com/about/versions)

# Instructions

1. Download the latest release by clicking on the "Releases" button on the right side of the page.
2. Start up the application and head to the Settings page.
3. Tweak the settings to your needs including which echelons to deploy, corpse dragging behavior, etc.
4. Now head back to the Home page and press the "Start" button to begin the process of setting up the necessary permissions to be used by the bot.
5. When done, pressing the "Start" button again will bring up the floating overlay button.
6. Head into Girls' Frontline and start the bot on the home screen. Recommended to put the floating overlay button on the bottom left.

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
