# Eye Boy Island (Android)
Liquid glass Dynamic Island overlay for the Samsung S24 FE. Say "Eye Boy, ..." to give commands. Uses Shizuku to grant its own permissions.

## Get the APK without Android Studio
1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the repo's Actions tab and run "Build APK" (it also runs on every push).
3. When it finishes, open the run and download the artifact `EyeBoyIsland-apk`. Unzip it and install app-debug.apk.

## Or build in Android Studio
Open this folder, let Gradle sync, then Build > Build APK(s).

## First run on the phone
1. Install and start Shizuku (wireless debugging; restart it after each reboot).
2. In Eye Boy: 1 Open Shizuku, 2 Allow Eye Boy, 3 Grant all permissions, 4 Start Eye Boy island.
