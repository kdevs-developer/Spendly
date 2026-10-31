# Spendly: fixes and audit

NOT COMPILED. There was no Android SDK where this was edited. Sync in Android Studio and expect to fix a small compile error or two.

## 1. What was wrong, and what changed

### Forgot password: reset mail not arriving
Code was fine (`sendPasswordResetEmail`). Two real problems:
- A stale message (`note`) hid the real result/error on the screen, and the text promised "Reset link sent" even when no account exists. Now the note is cleared on each try, errors are specific (bad email, no internet, too many attempts) and the success text is honest.
- The rest is Firebase console config, check these:
  1. Authentication > Settings > User actions: "Email enumeration protection" ON means Firebase says "sent" for emails that have no account and sends nothing. Confirm the email exists under Authentication > Users with provider "Email/Password" (a Google-only or guest account will not get a password mail).
  2. Authentication > Templates > Password reset: make sure it is enabled; the default sender (noreply@<project>.firebaseapp.com) often lands in Spam/Promotions. For reliable delivery set a custom sender domain or SMTP there.

### Add friend: "No one has that code" (typing and QR)
That message only appears when the server says `userCodes/<code>` does not exist. The old code wrote that document inside `runCatching` and ignored any failure, so a code could be shown on screen that nobody could look up. Now:
- The code is registered first (with collision handling), saved on the profile only after that works, and Add friend shows the real reason plus a "Try again" button if it fails.
- Lookups read from the server (not a stale cache) and show permission/offline errors.
- Most likely cause on your side: `firestore.rules` in the Firebase console is not the one in this project. Deploy: `firebase deploy --only firestore:rules,storage`. Then open Add friend once on each phone and check the `userCodes` collection has a document per user.

### Friends could never complete (found while fixing the above)
Accepting a request read the other person's `users/<uid>` document, which your own rules forbid. So Accept failed, and the sender never got the friend either. Requests now carry the sender's code/name and the receiver's name/code; no foreign profile reads. The sender is also added live as soon as the request is accepted (not only on next app start). Requests from blocked people are hidden.

### Delete all my data
Causes: (1) Firebase refuses `delete()` on a login that is not recently signed in, and the old code had already wiped the data by then; (2) it never touched requests, chats, or photos; (3) one slow delete at a time, and offline it would hang.
Now: Settings > Delete > a full "Delete my data" screen asks for your password (or the Google sheet) first, then deletes in batches: requests, your photos, leaves chats, all private collections, friend code, profile, push token, then the login. Needs internet and says so. Rules added so you can delete your own chat photos.
Optional `cleanupDeletedUser` Cloud Function (functions/index.js, Blaze plan) removes empty chats and their messages, which phones cannot.
Still remains after deletion: messages you already sent in other people's chats, and the offline cache on the phone (clear app data).

### Rules tightened
`userCodes` can no longer be listed (anyone could download every user id and name). Request answers can only change `status`, and only the receiver can set it. Chats cannot change type/creator. Splits can only be edited by their creator. Storage: only the owner can upload/delete their own `*_uid.jpg`.

### Strings, colours and values in XML
- `res/values/strings.xml`: 577 strings (UI text, errors, labels, seed category names). `colors.xml`: every colour (light/dark theme pairs, swatches, gradients, viewer, QR). `config.xml`: lock timeout and carousel timing.
- Kotlin reads them through `str(R.string.x, args)` and `clr(R.color.x)` (data/Res.kt), which work in composables, lambdas and top-level lists alike. `Res.init` runs first in `SpendlyApp`.
- Note: the app theme is chosen in Settings, not by Android night mode, so light and dark colours are named pairs (`ink_light` / `ink_dark`) rather than a `values-night` folder.
- NOT moved: dp/sp sizes and spacing (hundreds of inline values), Firestore field names, route strings, icon keys (those are identifiers, not text), and two sentences containing a quote inside a template were hand-converted.
- Lists such as welcome slides are built once, so a runtime language change needs an app restart.

### Build
Keystore path and password were hard-coded in `app/build.gradle.kts`. They now come from `keystore.properties` (copy `keystore.properties.example`; it is git-ignored). Without that file Android's normal debug key is used, which has a different SHA-1: Google sign-in then needs that SHA-1 added in Firebase.

## 2. Other flaws found (not fixed)

Security
- The old build file held a keystore password, and `debug.keystore`, `local.properties`, `.idea`, `build/`, `app/release/app-release.apk` were in the zip. If that password is used anywhere else, change it. If that keystore ever signs a store release, treat it as exposed. Run `git rm --cached debug.keystore`.
- `google-services.json` is committed. The API key is not secret but restrict it (package + SHA-1) in Google Cloud console and enable App Check.
- Anyone can create a chat with anyone (no friendship check) and send unlimited friend requests/reports: spam vector. Block is client-side only; blocked people can still write to you.
- Storage rules do not check chat membership; any signed-in user with a URL can read a chat photo.
- Any chat member can remove other members or rename the chat.
- Messages keep the sender's name after account deletion.

Reliability
- "Offline-first" is not true for writes: every `.await()` on a Firestore write only finishes when the server confirms, so Save buttons spin forever offline. Remove the `await()` (or use a timeout) for writes.
- `runCatching { ... }` is used around suspend calls in many places, which swallows errors and cancellation.
- No runtime request for notification permission (Android 13+), so push never shows. The "chat" notification channel is only created when a message arrives in the foreground, so background pushes on Android 8+ can be dropped until then. Create it in `SpendlyApp.onCreate`.
- Only one FCM token per user: a second device overwrites the first.
- Cloud Function sends pushes one after another per member and ignores failures (stale tokens never cleaned).
- Recurring items are created on app open; two devices opening together create duplicates. Each backfilled item is an awaited single write.
- All transactions and all messages are loaded in memory (no paging); balances are recomputed over every transaction on each draw. Fine for hundreds, slow for tens of thousands.
- Guest accounts: "Sign out" says "Your data stays safe in your account", which is false for a guest (the data is unreachable afterwards).
- Offline cache stays on the phone after Delete.

Product / release
- Release build type has no signing config; `versionCode` 1; minify on with only model keep rules, test a release build (Credential Manager, ML Kit scanner).
- Passwords only need 6 characters, no email verification.
- Privacy policy has a hard-coded date and no real contact; Play requires a working contact and a web-reachable policy URL.
- README says it was never compiled; there are no tests (money split maths in `Util.kt` is the first thing to cover).
- Project folder contained a stale nested copy `spendly-android/spendly-android/` (four files differ). It is left out of this zip; delete it in your repo.
