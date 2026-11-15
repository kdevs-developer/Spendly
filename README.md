# Spendly

An offline-first Android expense tracker with budgets, goals, bill reminders, split expenses, and friend chat. Built with Kotlin, Jetpack Compose, and Firebase.

**Package:** `com.kdevs.spendly` · **Currency:** INR (money is stored as whole paise)

<!-- Add screenshots here, for example:
<p align="center">
  <img src="docs/screenshots/home.png" width="22%" />
  <img src="docs/screenshots/reports.png" width="22%" />
  <img src="docs/screenshots/splits.png" width="22%" />
  <img src="docs/screenshots/chat.png" width="22%" />
</p>
-->

## Features

**Track money**
- Transactions with categories, sub-categories, colours, and icons
- Multiple accounts, each with its own default icon and colour
- Budgets, savings goals, recurring transactions, bills, and subscriptions
- Reports and insights
- Backup of your data

**Share and split**
- Friends added by code, QR code, or scan
- Split ledger, add expense, and settle up
- Chat with friends and groups, with images and message actions

**Everyday use**
- Offline-first: works without a connection through the Firestore local cache
- Sign in with Email/Password, Google, or as a guest (Anonymous)
- Light, Dark, or System theme (Settings > Appearance)
- Biometric lock with fingerprint, face, or screen lock (Settings > Security). The app asks again after 30 seconds away
- Animated splash screen and an adaptive launcher icon with a themed monochrome layer
- Push notifications through Cloud Messaging (optional)

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Backend | Firebase Authentication, Cloud Firestore, Cloud Storage, Cloud Messaging |
| Server code | Cloud Functions (Node.js) for push notifications |
| Build | Gradle (Kotlin DSL), Android Studio Ladybug or newer, JDK 17 |

## Getting started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- A Firebase project (the free plan is enough to start)
- Node.js and the Firebase CLI, only if you want push notifications or to deploy rules

### Setup

1. **Open the project.** Open this folder in Android Studio and let Gradle sync.
2. **Create a Firebase project.** In the [Firebase console](https://console.firebase.google.com), add an Android app with the package name `com.kdevs.spendly`, then download `google-services.json` into `app/`.
3. **Turn on the Firebase services.** Enable Authentication (Email/Password, Anonymous, and Google), Firestore, Storage, and Cloud Messaging.
4. **Set up Google sign-in.** Add your debug and release **SHA-1 and SHA-256** fingerprints to the Android app in Project settings, then download `google-services.json` again. The file must list a web client (`client_type` 3).
5. **Deploy the security rules.**
   ```bash
   firebase deploy --only firestore:rules,storage
   ```
6. **(Optional) Deploy push notifications.**
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```
7. **Run.** Select the `app` run configuration and press Run.

To get your debug SHA fingerprints:

```bash
./gradlew signingReport
```

## Project structure

```
Spendly/
├── app/                      Android application module
│   └── src/main/
│       ├── java/.../
│       │   ├── nav/          Nav.kt: every route and screen transition
│       │   ├── data/         Repo.kt (Firestore, Auth, Storage), Util.kt (money formatting, split maths)
│       │   └── ui/
│       │       ├── screens/  All screens, grouped as below
│       │       ├── components/  Shared components (for example Qr.kt)
│       │       └── splash/   SplashOverlay.kt: animated splash
│       └── res/              Strings, colours, config, icons, themes
├── functions/                Cloud Functions for push notifications
├── firestore.rules           Firestore security rules
├── storage.rules             Storage security rules
├── firebase.json             Firebase configuration
└── FIXES_AND_AUDIT.md        Notes on fixes from the latest audit
```

### Screens (`ui/screens/`)

| Group | Contents |
|---|---|
| Auth | Welcome carousel, sign in, Privacy, Terms |
| Main | Home, activity, reports, accounts |
| Money | Transactions, accounts, budgets, goals, recurring, bills, subscriptions, insights, backup |
| Pickers | Filter, select, bulk, category and sub-category, colour, icon, account, date |
| Chat | Chats, friends, conversation, message actions, image viewer, group info |
| Splits | Ledger, add expense, settle up |
| Settings | Appearance, security, and preferences |

## How it works

- **Money:** all amounts are stored as whole paise (integers), never floating point. `data/Util.kt` handles formatting and split calculations.
- **Data access:** every Firestore, Auth, and Storage call goes through `data/Repo.kt`. After every sign-in and app start, `Repo.ensureProfile()` fills in a missing name, friend code, and starter categories.
- **Navigation:** `nav/Nav.kt` holds all routes. Pickers return their result with `NavController.setPicked`, and the receiving screen reads it with `Picked`.
- **Offline:** the Firestore local cache lets the app read and write without a connection and sync later.

## Design rules

- Every add, edit, filter, select, and bulk action is its own full screen. Dialogs only ask yes or no.
- Buttons use a soft press (a small shrink and a faint shade in the button's own shape) instead of a ripple. A disabled or busy button stays solid.
- Toggles such as Expense / Income and Sign in / Create account use a sliding pill.
- Account types each have a default icon and colour. Colours include four gradients stored as keys (`g_sunset`, and so on).

## Customising

| What | Where |
|---|---|
| All text | `res/values/strings.xml` (read in Kotlin with `str(R.string.name)`) |
| Colours | `res/values/colors.xml` (read with `clr(R.color.name)`) |
| Timings | `res/values/config.xml` |
| Launcher icon | `res/mipmap-anydpi-v26` (adaptive), `ic_launcher-playstore.png` in `app/` (512 px Play Store icon) |
| Splash | `ui/splash/SplashOverlay.kt` and `Theme.Spendly.Starting` in `themes.xml` |

## Security and privacy

- Chat messages are protected only by the Firestore and Storage rules in `firestore.rules` and `storage.rules`. There is **no end-to-end encryption**.
- History, favourites, and settings are stored on your own Firebase project. Review the rules before using the app with real data.
- Never commit `google-services.json` with real keys to a public repository if you want to keep your project private. Add it to `.gitignore` if needed.

## Status

> This project was written without an Android SDK, so it has not been compiled. Expect a few small compile fixes on the first sync.
> <!-- Remove this note once the project builds cleanly. -->

## Roadmap

<!-- Fill this in with your own plans, for example:
- [ ] Undo bar after deleting a transaction
- [ ] Export to Excel and PDF
- [ ] Home screen widget
-->

## Contributing

Issues and pull requests are welcome. For larger changes, please open an issue first to discuss what you'd like to change.

## License

<!-- Choose a license and add a LICENSE file, for example MIT or Apache-2.0. -->
No licence has been chosen yet.
