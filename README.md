# Spendly (com.kdevs.spendly)

Android expense tracker. Kotlin, Jetpack Compose, Firebase. Money is whole paise (INR). Offline-first through the Firestore local cache. Light theme only.

## Set up
1. Open this folder in Android Studio (Ladybug or newer, JDK 17). Let it sync; it creates the Gradle wrapper.
2. In the Firebase console create a project, add an Android app with package `com.kdevs.spendly`, and download `google-services.json` into `app/`.
3. Turn on Authentication (Email/Password and Anonymous), Firestore, Storage and Cloud Messaging.
4. Deploy the rules: `firebase deploy --only firestore:rules,storage`.
5. Optional push notifications: `cd functions && npm install && firebase deploy --only functions`.
6. Run the `app` configuration.

## Layout
- `nav/Nav.kt` – every route and the screen transitions.
- `ui/screens/` – Auth (welcome carousel, sign in, Privacy and Terms), Main (home, activity, reports, accounts), Money (transactions, accounts, budgets, goals, templates, recurring), Pickers (filter, select, bulk, category and sub-category, colour, icon, account, date), Chat (chats, friends, conversation, message actions, image viewer, group info), Splits (ledger, add expense, settle up), Settings.
- `res/` – adaptive launcher icon (`mipmap-anydpi-v26`, with themed monochrome layer), legacy PNG icons, notification icon, colours and theme. `ic_launcher-playstore.png` (in `app/`) is the 512px Play Store icon.
- `ui/splash/SplashOverlay.kt` – animated splash (ring and rupee draw in, coin fills, wordmark rises, fade into the app). `Theme.Spendly.Starting` in `themes.xml` is the plain blue system splash that hands over to it.
- `data/Repo.kt` – all Firestore, Auth and Storage access. `data/Util.kt` – money formatting and split maths.
- `firestore.rules`, `storage.rules` – chat is protected by these rules only (no end-to-end encryption).

## Design rules followed
- Every add, edit, filter, select and bulk action is its own full screen. Dialogs only ask yes or no.
- Pickers return their result with `NavController.setPicked` and the receiving screen reads it with `Picked`.
- Account types have their own default icon and colour. Colours include four gradients stored as keys (`g_sunset`, ...).

## Not verified
This project was written without an Android SDK, so it has not been compiled. Expect a few small compile fixes on the first sync.
