# Installation Guide

## Prerequisites

Install:

- Android Studio
- Android SDK
- JDK compatible with the Android project
- Node.js and npm
- MongoDB (local or hosted MongoDB Atlas)
- Git

## 1. Clone the Repository

```bash
git clone <YOUR_PUBLIC_REPOSITORY_URL>
cd <YOUR_REPOSITORY_FOLDER>
```

## 2. Backend Setup

```bash
cd server
npm install
```

Create `server/.env`:

```env
PORT=5000
MONGO_URI=mongodb://127.0.0.1:27017/feedback_app
JWT_SECRET=replace_with_a_secure_secret
```

Do not commit this `.env` file.

Start the server:

```bash
npm start
```

The local API will normally be available on port `5000`.

## 3. MongoDB

### Local MongoDB

Make sure MongoDB is running before starting the server.

Example:

```env
MONGO_URI=mongodb://127.0.0.1:27017/feedback_app
```

### MongoDB Atlas

Use your Atlas connection string in `MONGO_URI`. Keep credentials private.

## 4. Seed/Test Users

Create your own development Admin/Teacher accounts using your project's seed mechanism or database setup.

Do **not** put reference-portal credentials in a public repository.

## 5. Android Setup

1. Open Android Studio.
2. Choose **Open**.
3. Select the repository's `android/` directory.
4. Wait for Gradle sync to complete.
5. Configure the application's Retrofit/API base URL.

### Emulator

An Android emulator cannot use `localhost` to access a backend on your development computer.

Use:

```text
http://10.0.2.2:5000/
```

### Physical Device

The phone and development computer should be on the same network when using a locally hosted backend.

Use your computer's LAN IP, for example:

```text
http://192.168.x.x:5000/
```

Allow port `5000` through the firewall if required.

### Hosted Backend

For a deployed server, configure Retrofit with the HTTPS deployment URL and keep the trailing `/` required by Retrofit.

## 6. Run Android App

Connect an Android device with USB debugging enabled or start an emulator.

In Android Studio:

**Run → Run 'app'**

## 7. Build APK

From Android Studio:

**Build → Build APK(s)**

or from the Android project directory:

### Windows

```bash
gradlew.bat assembleDebug
```

### macOS/Linux

```bash
./gradlew assembleDebug
```

The debug APK is normally generated under:

```text
app/build/outputs/apk/debug/
```

## 8. Testing Checklist

Verify:

- Admin login
- Teacher login
- persistent session and logout
- deactivated teacher login block
- teacher management
- teacher form creation → Pending
- Admin approval/rejection
- form activation/deactivation
- form sharing and QR
- Present submission
- Absent submission
- all seven question types
- rating below 8 requires reason
- duplicate submission is blocked
- inactive form is blocked
- Summary/Individual/Lower Feedback
- re-feedback
- CSV export/share
- delete form behavior

## Common Problems

### Android cannot connect to local server

Do not use `localhost` from the emulator. Use `10.0.2.2`.

For a physical device, use the computer's LAN IP and check the firewall.

### 401 Unauthorized

Check that the JWT token exists and protected requests send:

```text
Authorization: Bearer <token>
```

### Server cannot connect to MongoDB

Check `MONGO_URI` and confirm MongoDB/Atlas access is available.

### Changes are not visible on deployed backend

Push the latest server code and wait for the hosting service to finish redeployment.
