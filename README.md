# Sip & Smile 💧😊

A cute Android app + home-screen widget that reminds you to **sip water** and **smile**,
always using your name. Reminders come every 15 minutes (or 30 / 60) **only inside the time window
you choose** (for example 8:00 AM to 9:00 PM).

* 7 hand-drawn buddies (water drop, bird, butterfly, cloud, paper plane, bee, moon), each with its own animated scene
* Soft chime / bird tweet sounds (included, no extra files to make)
* Notification when the screen is off, widget update when the screen is on
* Daily glass counter, streak, pause, dark mode

## What's new in 1.1

- **One combined reminder:** every reminder asks for a sip *and* a smile, with **I drank** and **I smiled** buttons (notification, scene, widget).
- **Your own water goal:** 1 to 30 glasses a day, glass size 150 to 500 ml, optional weight-based suggestion, undo for accidental taps. The counter never shows 10/8; after the goal it shows `8/8 +2`.
- **Smile selfie:** tap *I smiled*, say yes, take a selfie with a 3-2-1 countdown, get the "We captured the moment" polaroid. Photos are private to the app, with **Save to gallery** (Pictures/SipAndSmile) and **Share**.
- **Smile Album, Stats & badges**, buddy mood, snooze 10 min, evening summary notification.
- **Appearance:** Light / Dark / Phone mode and 5 accent colours. Change your name any time in Settings.

New permission: CAMERA (asked only when you say yes to a selfie).

## Option A: build with Android Studio (on your laptop)

1. Install **Android Studio** (free): https://developer.android.com/studio . Accept all the default options.
2. Open Android Studio, choose **Open**, and pick this folder (the one that contains `settings.gradle.kts`).
3. Wait for the first **Gradle sync** (bottom bar). It downloads what it needs, which can take 5-15 minutes the first time.
   If it asks to **install missing SDK / platform 35**, click the link and accept.
4. Build the app: menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   When it finishes, click **locate** in the popup. The file is `app/build/outputs/apk/debug/app-debug.apk`.
5. Send `app-debug.apk` to your phone (USB cable, Google Drive, email or WhatsApp to yourself).

### Or run straight on the phone with a cable
Phone: Settings > About phone > tap **Build number** 7 times > back > **Developer options** > turn on **USB debugging**.
Plug in the phone, allow the prompt, pick your phone at the top of Android Studio and press the green **Run** button.

## Option B: no Android Studio, build in the cloud (free)

1. Make a free account at https://github.com and create a **new private repository** called `sip-and-smile`.
2. Click **uploading an existing file** and drag in everything from this folder (including the `.github` folder). Commit.
   (If the `.github` folder did not upload: **Add file > Create new file**, name it `.github/workflows/build-apk.yml`
   and paste the contents of that file from this project.)
3. Open the **Actions** tab, choose **Build & Release APK**, press **Run workflow**. Wait about 5 minutes for the green tick.
4. Open the finished run, scroll to **Artifacts**, download **SipAndSmile-apk**, unzip it: inside is the `.apk`.

### Publish a Release (a download link you can open on your phone)

* **Easiest:** Actions > Build & Release APK > **Run workflow**, type a version such as `v1.0.0` in the box, run.
* **Or with a tag:** push a tag that starts with `v` (for example `git tag v1.0.0 && git push origin v1.0.0`).

When it finishes, open the repository's **Releases** page (right side of the repo home). The APK is attached
to the release, so you can open that page on your phone and tap the file to install it.

## Install the APK on your phone

1. Open `app-debug.apk` on the phone (from Downloads / Drive / WhatsApp).
2. Android asks to allow **Install unknown apps** for that app: allow it once.
3. If **Play Protect** warns, tap **Install anyway** (the app is your own build).
4. Open **Sip & Smile**.

## First launch

1. Type your name, tap **Nice to meet you!**.
2. Tap **Allow** on the three cards (notifications, exact timing, no battery sleeping).
3. Pick your time window, e.g. 8:00 AM to 9:00 PM, and the interval.
4. On the Home screen tap **Add widget**, or long-press your home screen > Widgets > **Sip & Smile**.
5. Tap **Test** to see a reminder right away.

## If reminders arrive late

Some phones are strict with background apps:
* **Xiaomi / Redmi / Poco**: Settings > Apps > Manage apps > Sip & Smile > **Autostart ON**, Battery saver > **No restrictions**
* **Oppo / Realme / OnePlus**: Settings > Battery > Sip & Smile > **Allow background activity**, also allow **Auto launch**
* **Vivo / iQOO**: Settings > Battery > Background power consumption > Sip & Smile > **Allow**
* **Samsung**: Settings > Apps > Sip & Smile > Battery > **Unrestricted**, and remove it from "Sleeping apps"

## Project map

| Path | What it is |
|------|------------|
| `app/src/main/java/com/sipsmile/app/reminder` | alarm chain, notifications, boot restore |
| `app/src/main/java/com/sipsmile/app/widget` | the Glance home-screen widget |
| `app/src/main/java/com/sipsmile/app/ui` | Home, Settings, Onboarding, scene screen |
| `app/src/main/java/com/sipsmile/app/ui/scenes` | the 7 animated scenes (all drawn in code) |
| `app/src/main/java/com/sipsmile/app/ui/chars` | character art (generated file `ArtData.kt`) |
| `app/src/main/res/raw` | the soft sounds (.wav) |
| `tools/` | scripts that generated the art and sounds (you do not need to run them) |
