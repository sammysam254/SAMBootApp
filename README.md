# SAM Boot Screen & Live Wallpaper
### by Samson Matata Mwinzi — Vertext Digital

---

## What This App Does

| Feature | Description |
|---------|-------------|
| 🎬 Boot Splash | Cinematic S→A→M letter-by-letter animation with sound on every app launch |
| 🔔 Boot Receiver | Triggers SAM splash automatically after phone boots up |
| 🌟 Live Wallpaper | Animated SAM logo with gold pulse rings, particles, scanlines on your home/lock screen |
| 🔊 Sounds | Custom WAV tones for each letter + deep boot thud + 3-note chime |

---

## How to Build & Install

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Kotlin 1.9+
- A physical Android device (Samsung recommended) running Android 8.0+

### Steps

1. **Open in Android Studio**
   ```
   File → Open → select the SAMBootApp folder
   ```

2. **Sync Gradle**
   - Click "Sync Now" when prompted
   - Wait for dependencies to download

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Phone**
   - Enable Developer Options + USB Debugging on your Samsung
   - Connect via USB
   - Click ▶ Run in Android Studio
   - OR: `adb install app-debug.apk`

---

## Setting the Live Wallpaper

After installing:

1. Open the **SAM** app
2. Tap **"SET AS LIVE WALLPAPER"**
3. The system wallpaper picker opens with SAM pre-selected
4. Tap **"Set wallpaper"** → choose Home, Lock, or Both

---

## Boot Screen Behaviour

- **On every app launch:** SAM splash plays (S→A→M with sounds, then your intro, then fades to home)
- **On phone boot:** The `BootReceiver` fires and launches the SAM splash automatically

> ⚠️ **Samsung Note:** Samsung phones show their own boot animation *before* Android loads.  
> This app plays **after** the Samsung logo disappears — as soon as the OS starts.  
> To replace Samsung's own boot animation, you would need root + a custom boot animation file.  
> This app gives you the closest possible experience **without root.**

---

## File Structure

```
SAMBootApp/
├── app/src/main/
│   ├── java/com/samson/bootscreen/
│   │   ├── SplashActivity.kt      ← Boot animation controller
│   │   ├── MainActivity.kt        ← Home screen + wallpaper setter
│   │   ├── SamWallpaperService.kt ← Live wallpaper engine (Canvas drawing)
│   │   └── BootReceiver.kt        ← Fires SAM splash on device boot
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_splash.xml
│   │   │   └── activity_main.xml
│   │   ├── raw/
│   │   │   ├── tone_s.wav         ← Letter S sound
│   │   │   ├── tone_a.wav         ← Letter A sound
│   │   │   ├── tone_m.wav         ← Letter M sound
│   │   │   ├── tone_boot.wav      ← Deep boot thud
│   │   │   └── tone_chime.wav     ← Final 3-note chime
│   │   ├── drawable/              ← Rings, gradients, progress bar, vignette
│   │   ├── xml/wallpaper.xml      ← Live wallpaper metadata
│   │   └── values/themes.xml
│   └── AndroidManifest.xml
└── README.md
```

---

## Customization

Edit `SplashActivity.kt` to change animation timing.  
Edit `SamWallpaperService.kt` to change colors, particle count, or ring speed.  
Replace WAV files in `res/raw/` with your own sounds.

---

*Built with ❤️ for Samson Matata Mwinzi — Vertext Digital, Ruiru Kenya*
