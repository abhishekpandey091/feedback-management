# API Documentation

> Base prefix: `/api`

Protected endpoints require:

```text
Authorization: Bearer <jwt>
```

Student/public endpoints do not require authentication.

The exact response objects contain the application's Mongoose/model fields in addition to the summarized fields below.

## Authentication

### Login

**POST** `/api/auth/login`

Auth: Public

Request:

```json
{
  "email": "admin@example.com",
  "password": "example-password"
}
```

Response summary:

```json
{
  "token": "<jwt>",
  "user": {
    "role": "admin"
  }
}
```

## Teachers

### List Teachers

**GET** `/api/teachers`

Auth: Admin

Returns the teacher list.

### Create Teacher

**POST** `/api/teachers`

Auth: Admin

Request summary:

```json
{
  "fullName": "Example Teacher",
  "email": "teacher@example.com",
  "password": "example-password"
}
```

### Toggle Teacher Active Status

**PATCH** `/api/teachers/:id/toggle-active`

Auth: Admin

Toggles whether the teacher can login.

### Delete Teacher

**DELETE** `/api/teachers/:id`

Auth: Admin

Deletes the selected teacher according to backend validation.

## Forms

### Create Form

**POST** `/api/forms`

Auth: Admin or Teacher

Request example:

```json
{
  "title": "Lecture Feedback",
  "description": "Feedback for today's lecture",
  "assignedTo": "<teacher-id>",
  "allowedBatches": ["Batch A"],
  "questions": [
    {
      "questionText": "How would you rate the lecture?",
      "type": "star_rating",
      "options": [],
      "maxStars": 10,
      "required": true
    }
  ]
}
```

Behavior:

- Admin-created form: approved/inactive and assigned to a teacher.
- Teacher-created form: pending/inactive and assigned to the teacher.

### Teacher's Forms

**GET** `/api/forms/my-forms`

Auth: Teacher

Returns forms assigned to the logged-in teacher.

### List Forms

**GET** `/api/forms`

Auth: Admin or Teacher

Supported query parameters in the current implementation:

- `page`
- `limit`
- `teacherId` (Admin filter)

Example response summary:

```json
{
  "forms": [],
  "pagination": {
    "page": 1,
    "limit": 10,
    "total": 0,
    "totalPages": 0,
    "hasMore": false
  }
}
```

### Get Form

**GET** `/api/forms/:formId`

Auth: Admin/authorized Teacher

Returns one form.

### Approve Form

**PATCH** `/api/forms/:formId/approve`

Auth: Admin

Approves a pending/rejected teacher form. Approval does not automatically activate the form.

### Reject Form

**PATCH** `/api/forms/:formId/reject`

Auth: Admin

Request:

```json
{
  "reason": "Please update the questions."
}
```

The form becomes rejected/inactive and stores the rejection reason.

### Activate Form

**PATCH** `/api/forms/:formId/activate`

Auth: Admin or authorized assigned Teacher

Marks an approved form active and records `activatedAt`.

### Deactivate Form

**PATCH** `/api/forms/:formId/deactivate`

Auth: Admin or authorized assigned Teacher

Marks the form inactive.

### Update/Edit Form

**PATCH/PUT** `/api/forms/:formId`

Auth: According to implemented role/ownership validation.

Used by the Android edit-form flow. Teacher editing follows the application's approval lifecycle.

### Delete Form

**DELETE** `/api/forms/:formId`

Auth: Admin or authorized Teacher

Deletes a form and its associated responses according to the implemented cascade behavior.

## Public Student Forms

### Get Public Form

**GET** `/api/public/forms/:formId`

Auth: Public

Requirements:

- form exists
- form is approved
- form is active
- activation has not expired

Possible statuses include:

- `200` form returned
- `404` form not found
- `410` form deactivated/expired

### Submit Feedback

**POST** `/api/public/forms/:formId/responses`

Auth: Public

Request summary:

```json
{
  "studentName": "Student Name",
  "batch": "Batch A",
  "enrollmentNumber": "EN123",
  "answers": [
    {
      "questionId": "<question-id>",
      "answer": "Example answer"
    }
  ]
}
```

Validation includes:

- required student fields
- allowed batch
- required questions
- valid MCQ/dropdown options
- valid checkbox options
- valid Yes/No values
- valid star range
- feedback business rules such as low-rating reason/duplicate handling in the corresponding implementation

### Re-Feedback

The student/public response routes include the application's re-feedback retrieval/submission flow using `formId` and `responseId`.

Behavior:

- previous answers are loaded/prefilled
- response is updated rather than creating an unrelated response
- previous answers can be retained
- Yes → No is rejected
- active-form rules apply

Use the route paths defined in `studentRoutes.js` as the source of truth if they differ from a deployment version.

## Responses

### Responses for Form

**GET** `/api/responses/form/:formId`

Auth: Admin or authorized Teacher

Response summary:

```json
{
  "totalResponses": 1,
  "responses": []
}
```

### Response Summary

**GET** `/api/responses/form/:formId/summary`

Auth: Admin or authorized Teacher

Returns per-question analytics.

Star rating summary includes:

```json
{
  "average": 8.5,
  "lowerCount": 1
}
```

MCQ/dropdown/Yes-No and checkbox questions return answer counts.

### Lower Feedback

**GET** `/api/responses/form/:formId/lower-feedback`

Auth: Admin or authorized Teacher

Returns responses containing a star rating below 8.

### Individual Response

**GET** `/api/responses/:responseId`

Auth: Admin or authorized Teacher

Returns one response after verifying access to its form.

### Export Responses

The response API includes the project's CSV export endpoint used by Android for response export/share.

The exact export route in `responseRoutes.js` should be used as the source of truth for the final deployed version.

## Common HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | Request successful |
| 201 | Resource created |
| 400 | Validation/bad request |
| 401 | Missing/invalid authentication |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Duplicate/conflict |
| 410 | Form inactive/expired |
| 500 | Server error |

## Question Object

```json
{
  "questionText": "Question",
  "type": "short",
  "options": [],
  "maxStars": 10,
  "required": true
}
```

Supported types:

```text
short
paragraph
mcq
checkbox
dropdown
star_rating
yes_no
```

## Security Notes

- Never commit real JWT secrets.
- Never commit database credentials.
- Passwords should be stored as hashes.
- Protected routes must validate JWT and role/ownership.
- Public student routes should expose only the information needed to complete feedback.
