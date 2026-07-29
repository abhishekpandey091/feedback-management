# System Explanation

## Overview

Feedback Management is an Android + backend system for collecting and analyzing academic feedback. It provides three user flows:

- **Admin** — manages teachers and controls form approval/administration.
- **Teacher** — creates/manages assigned forms and reviews responses.
- **Student** — submits feedback without creating an account.

The Android application communicates with a Node.js/Express REST API. MongoDB stores users, forms and responses. Admin/Teacher protected endpoints use JWT Bearer authentication.

## Architecture

```text
Android App (Kotlin/XML)
        |
        | Retrofit / JSON / JWT
        v
Node.js + Express REST API
        |
        | Mongoose
        v
      MongoDB
```

## Authentication

Admin and Teacher users login using the Android application.

On successful authentication:

1. Backend validates credentials.
2. Backend returns a JWT and user information.
3. Android stores the token/session locally.
4. Protected requests send `Authorization: Bearer <token>`.
5. Navigation is selected according to the returned role.

Students do not login.

## Roles

### Admin

Admin can:

- manage teachers
- create/assign forms
- view forms
- approve/reject pending forms
- activate/deactivate forms
- view responses and analytics
- export/share response data
- generate/share QR links
- delete forms

### Teacher

Teacher can:

- create own feedback forms
- view created/assigned forms
- edit/manage allowed forms
- activate/deactivate approved assigned forms
- share forms
- generate QR codes
- view responses
- view lower feedback
- use re-feedback workflow

Teacher-created forms begin as **pending** and **inactive**.

### Student

Student requires no account.

Student can:

- open an active form using a link, QR or form ID flow
- enter identifying details required by the form
- choose Present/Absent
- answer questions
- submit feedback
- submit re-feedback when provided the corresponding link/QR

## Form Lifecycle

### Teacher-created form

```text
Teacher creates
      ↓
Pending + Inactive
      ↓
Admin Approves
      ↓
Approved + Inactive
      ↓
Activate
      ↓
Active
      ↓
Student submissions
```

Admin can also reject a pending form with a reason.

### Admin-created form

Admin-created forms can be created as approved but remain inactive until activation.

## Activation

An approved form can be activated for student access.

The system tracks `activatedAt`. Active public forms are automatically considered expired after the configured 15-minute period when checked/fetched.

Manual activation/deactivation is also supported according to role permissions.

## Question Types

The form system supports:

1. `short`
2. `paragraph`
3. `mcq`
4. `checkbox`
5. `dropdown`
6. `star_rating`
7. `yes_no`

Questions may be required and option-based questions validate answers against configured options.

Star-rating questions use the configured maximum rating.

## Student Feedback Flow

```text
Open form
   ↓
Validate active/approved form
   ↓
Enter student information
   ↓
Select batch
   ↓
Present / Absent
   ↓
Answer questions (Present)
   ↓
Validate
   ↓
Submit
   ↓
Success
```

### Present

When Present, normal question validation applies.

### Absent

The application can hide/skip normal feedback questions and submit the attendance state according to the implemented flow.

### Low Rating

A star rating below 8 requires a written reason. Validation is enforced by the application/backend flow.

### Duplicate Prevention

The system prevents duplicate feedback for the same student/form/batch within the defined IST calendar-day rule.

## Responses & Analytics

Authorized Admin/Teacher users can access response data for forms they are allowed to view.

Response functionality includes:

- response list
- total responses
- question summary
- average star rating
- low-rating count
- option counts for MCQ/dropdown/Yes-No
- checkbox counts
- individual response view
- lower feedback view
- CSV export/share

Charts are rendered in the Android application for supported summary data.

## Lower Feedback

Lower Feedback identifies responses containing star ratings below the configured threshold of 8.

This provides a focused view of students whose feedback may need attention.

## Re-Feedback

Re-feedback allows a student to revisit an existing response through a form/response-specific flow.

The system:

- loads the previous response
- pre-fills previous answers
- retains previous answer information
- validates changes
- updates the response
- prevents a previous **Yes** answer from being changed to **No**

The form must satisfy the required active/access rules.

## Sharing and QR

Activated forms can be shared using Android's share intent.

QR codes encode the application's form/deep-link information so a student can open the corresponding feedback flow.

Re-feedback can similarly use a response-specific link/QR.

## Form Deletion

Deleting a form is intended to delete its associated responses as well. The UI asks for confirmation before destructive deletion.

## Android Navigation Map

```text
Login
├── Admin Dashboard
│   ├── Manage Teachers
│   ├── Create Form
│   └── Admin Forms
│       ├── Approve / Reject
│       ├── Activate / Deactivate
│       ├── Share / QR
│       └── Responses
│           ├── Summary
│           ├── Individual
│           └── Lower Feedback
│
├── Teacher Dashboard
│   ├── Create Form
│   └── My Forms
│       ├── Edit
│       ├── Activate / Deactivate
│       ├── Share / QR
│       └── Responses
│
└── Student Feedback
    ├── Open Form
    ├── Submit Feedback
    └── Re-Feedback
```

## Intentional Mobile Design

The Android application uses mobile-specific interactions such as:

- native sharing
- QR presentation
- phone-friendly forms
- Android navigation
- loading/error states
- responsive controls

The goal is to reproduce the product behavior while adapting interaction to a mobile application rather than reproducing a desktop web page exactly.
