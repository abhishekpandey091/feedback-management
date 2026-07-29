# Feedback Management System

Android + Backend implementation of an Academic Feedback Management System.

## Tech Stack

### Android
- Kotlin
- XML Views
- Retrofit
- JWT Bearer authentication
- MPAndroidChart
- ZXing QR generation

### Backend
- Node.js
- Express.js
- MongoDB
- Mongoose
- JWT authentication
- bcrypt

## Project Structure

```text
project/
├── android/              # Android Studio project
├── server/               # Node.js/Express backend
├── docs/
│   ├── installation.md
│   ├── explanation.md
│   ├── API.md
│   └── Env.example
├── README.md
└── .gitignore
```

## Main Features

### Admin
- Login and role-based dashboard
- Create/manage teachers
- Create and assign forms
- Approve/reject teacher-created forms
- Activate/deactivate forms
- Filter forms by teacher
- View response analytics
- Export/share responses
- Generate/share QR codes
- Delete forms and associated responses

### Teacher
- Login and role-based dashboard
- Create feedback forms
- View/manage assigned forms
- Edit forms
- Activate/deactivate approved assigned forms
- Share form link/QR
- View responses, analytics, and lower feedback
- Re-feedback workflow

### Student
- No login required
- Open feedback form using form ID/link/QR
- Present/Absent attendance flow
- Submit all supported question types
- Required low-rating reason
- Re-feedback with previous answers

## Supported Question Types

- Short answer
- Paragraph
- MCQ
- Checkbox
- Dropdown
- Star rating
- Yes / No

## Quick Start

### Backend

```bash
cd server
npm install
```

Create a `.env` file using `docs/Env.example`, then:

```bash
npm start
```

### Android

1. Open the `android/` directory in Android Studio.
2. Configure the API base URL for your backend.
3. Sync Gradle.
4. Run the application on an emulator or physical Android device.

For emulator testing against a backend running on the same computer, use `10.0.2.2` instead of `localhost`.

See [docs/installation.md](docs/installation.md) for complete setup instructions.

## Documentation

- `docs/installation.md` — installation and run instructions
- `docs/explanation.md` — system design, roles and flows
- `docs/API.md` — API reference
- `docs/Env.example` — environment variable template

## Security

Do not commit:
- `.env`
- real passwords or JWT secrets
- `local.properties`
- signing keys
- database credentials

## Demo Flow

1. Start backend and Android app.
2. Login as Admin.
3. Create/manage a teacher.
4. Login as Teacher and create a form.
5. Admin approves the pending form.
6. Teacher activates and shares the form.
7. Student submits feedback.
8. Verify duplicate/validation rules.
9. View Summary, Individual and Lower Feedback.
10. Perform re-feedback.
11. Export/share responses.
12. Deactivate the form and verify public access is blocked.
