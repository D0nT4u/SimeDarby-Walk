# SimeDarby Walk — offline Android demo

This project now contains a runnable **Java + Material 3 Views** demo, using the existing Android Studio project and cached dependencies. It is not the Kotlin/Firebase implementation described earlier in chat.

## Run

1. Open this project folder in Android Studio.
2. Choose **File → Sync Project with Gradle Files**.
3. Select the **app** run configuration and a running emulator or phone (Android 8 / API 26 or later).
4. Press **Run**. No Firebase configuration, Maps key or internet connection is required.

Gradle is configured to use Android Studio's full JetBrains JDK at `C:/Program Files/Android/Android Studio/jbr`. This fixes the incomplete VS Code Java runtime being chosen, which caused a missing `jlink.exe` build error. If Android Studio is installed elsewhere, update `org.gradle.java.installations.paths` in `gradle.properties`. The daemon criteria require JetBrains Java 25, matching this machine's Android Studio.

## Accounts

| Account | Email | Password |
| --- | --- | --- |
| Resident | resident@demo.com | Demo1234 |
| Guard | guard@demo.com | Demo1234 |
| Management | management@demo.com | Demo1234 |

Each fresh launch shows **Splash → Choose your role → Sign in**, even after previously signing in. Select Resident, Guard or Management from the dropdown, press **Continue**, then sign in using the filled demo credentials. Residents see **No account? Register now**. **Change role** returns to role selection. These are local demonstration credentials, not real authentication. Registering replaces the single local resident profile and uses the same demo password. Returning from another app keeps your current screen; recreation keeps the current authentication step and selected role. Preset personal names have been replaced with generic labels; names entered by the user remain intact.

## Demonstration flow

1. Sign in as Resident. Tap hazard markers for descriptions. Expired reports are hidden.
2. Start walk → choose a destination → select a contact and route → Start walk. Route recommendations count reports within 30 demo metres.
3. The simulated walker moves while the app is open. The smart light turns on within 40 demo metres of its coordinate. Pause the walker for two minutes or use **Test check-in** to show the 30-second check-in.
4. Open **Send SOS**, confirm block/level/zone, and hold the send button for two seconds. Cancel during the five-second countdown or let it create a local alert.
5. Choose **Open guard demo**. Acknowledge, mark on the way, then resolve. Resident status and the management figures read the same local data.
6. Switch to Management to review hazards, change their status and add known works. Debris and road closures require an end date.
7. Use **Profile → Reset demo data** before another presentation if needed.

Contacts accept the two registered sample numbers shown on the Contacts screen. Call buttons only display the demo number; no real call is placed.

## Scope

- The township map is a schematic, not Google Maps. No GPS, geofences, Firebase, FCM, background service or ESP32 is connected.
- Tracking and SOS are simulations on one device. Switching demo accounts preserves the demonstration; signing out ends the walk.
- Simulated movement pauses when the app is not visible. This is not an emergency-response system.
- Reports, contacts, the resident profile and alert statuses persist locally in app preferences.
- The official logo is not included. Place your original file at `app/src/main/res/drawable/sime_darby_property_logo.png`; the demo automatically displays it. No company logo is recreated.

## Command-line checks (PowerShell)

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
```

For emulator installation, use Android Studio Run. Some IDE-generated APKs are marked test-only; manual ADB installation of those requires `adb install -r -t <apk-path>`.
