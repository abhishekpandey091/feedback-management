const express = require("express");
const Form = require("../models/Form");
const Response = require("../models/Response");

const router = express.Router();

const FIFTEEN_MINUTES = 15 * 60 * 1000;

// Check whether form has expired
async function checkFormActive(form) {
  if (!form.isActive) return false;

  if (
    form.activatedAt &&
    Date.now() - new Date(form.activatedAt).getTime() >= FIFTEEN_MINUTES
  ) {
    form.isActive = false;
    await form.save();
    return false;
  }

  return true;
}

// PUBLIC: Get form
router.get("/forms/:formId", async (req, res) => {
  try {
    const form = await Form.findById(req.params.formId);

    if (!form || form.approvalStatus !== "approved") {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    const active = await checkFormActive(form);

    if (!active) {
      return res.status(410).json({
        message: "This feedback form is deactivated",
      });
    }

    res.status(200).json({
      message: "Form fetched successfully",
      form: {
        _id: form._id,
        title: form.title,
        description: form.description,
        questions: form.questions,
        allowedBatches: form.allowedBatches,
      },
    });
  } catch (error) {
    console.error("PUBLIC FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// PUBLIC: Submit feedback
// PUBLIC: Submit feedback
router.post("/forms/:formId/responses", async (req, res) => {
  try {
    const {
      studentName,
      batch,
      enrollmentNumber,
      attendanceStatus,
      lowRatingReason,
      answers,
    } = req.body;

    // Basic validation
    if (
      !studentName ||
      !batch ||
      !enrollmentNumber ||
      !["Present", "Absent"].includes(attendanceStatus) ||
      !Array.isArray(answers)
    ) {
      return res.status(400).json({
        message: "Student details and attendance status are required",
      });
    }

    const form = await Form.findById(req.params.formId);

    if (!form || form.approvalStatus !== "approved") {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    const active = await checkFormActive(form);

    if (!active) {
      return res.status(410).json({
        message: "This feedback form is deactivated",
      });
    }

    // Allowed batch validation
    if (
      form.allowedBatches.length > 0 &&
      !form.allowedBatches.includes(batch.trim())
    ) {
      return res.status(403).json({
        message: "Your batch is not allowed for this form",
      });
    }

    // ------------------------------------------------
    // SAME IST DAY DUPLICATE CHECK
    // ------------------------------------------------

    const now = new Date();

    // IST = UTC + 5:30
    const IST_OFFSET = 5.5 * 60 * 60 * 1000;

    const istNow = new Date(now.getTime() + IST_OFFSET);

    const startOfISTDay = new Date(
      Date.UTC(
        istNow.getUTCFullYear(),
        istNow.getUTCMonth(),
        istNow.getUTCDate(),
        0,
        0,
        0,
        0,
      ) - IST_OFFSET,
    );

    const endOfISTDay = new Date(startOfISTDay.getTime() + 24 * 60 * 60 * 1000);

    const existingResponse = await Response.findOne({
      formId: form._id,
      enrollmentNumber: enrollmentNumber.trim(),
      batch: batch.trim(),
      submittedAt: {
        $gte: startOfISTDay,
        $lt: endOfISTDay,
      },
    });

    if (existingResponse) {
      return res.status(409).json({
        message: "You have already submitted this form today",
      });
    }

    // ------------------------------------------------
    // PRESENT STUDENT VALIDATION
    // ------------------------------------------------

    if (attendanceStatus === "Present") {
      let hasLowRating = false;

      for (const question of form.questions) {
        const submittedAnswer = answers.find(
          (item) => item.questionId?.toString() === question._id.toString(),
        );

        // Required question
        if (question.required) {
          if (
            !submittedAnswer ||
            submittedAnswer.answer === "" ||
            submittedAnswer.answer === null ||
            submittedAnswer.answer === undefined ||
            (Array.isArray(submittedAnswer.answer) &&
              submittedAnswer.answer.length === 0)
          ) {
            return res.status(400).json({
              message: `Answer required: ${question.questionText}`,
            });
          }
        }

        if (!submittedAnswer) continue;

        // MCQ / Dropdown
        if (
          ["mcq", "dropdown"].includes(question.type) &&
          !question.options.includes(submittedAnswer.answer)
        ) {
          return res.status(400).json({
            message: `Invalid answer for: ${question.questionText}`,
          });
        }

        // Checkbox
        if (question.type === "checkbox") {
          if (!Array.isArray(submittedAnswer.answer)) {
            return res.status(400).json({
              message: `Invalid checkbox answer: ${question.questionText}`,
            });
          }

          const invalidOption = submittedAnswer.answer.some(
            (option) => !question.options.includes(option),
          );

          if (invalidOption) {
            return res.status(400).json({
              message: `Invalid option for: ${question.questionText}`,
            });
          }
        }

        // Yes / No
        if (
          question.type === "yes_no" &&
          !["Yes", "No"].includes(submittedAnswer.answer)
        ) {
          return res.status(400).json({
            message: `Invalid Yes/No answer: ${question.questionText}`,
          });
        }

        // Star rating
        if (question.type === "star_rating") {
          const rating = Number(submittedAnswer.answer);

          if (
            !Number.isInteger(rating) ||
            rating < 1 ||
            rating > question.maxStars
          ) {
            return res.status(400).json({
              message: `Invalid rating for: ${question.questionText}`,
            });
          }

          if (rating < 8) {
            hasLowRating = true;
          }
        }
      }

      // Rating below 8 requires reason
      if (
        hasLowRating &&
        (!lowRatingReason || lowRatingReason.trim().length === 0)
      ) {
        return res.status(400).json({
          message: "Reason is required for a rating below 8",
        });
      }
    }

    // ------------------------------------------------
    // ABSENT
    // ------------------------------------------------

    // Absent students don't submit question answers.
    const finalAnswers = attendanceStatus === "Absent" ? [] : answers;

    const response = await Response.create({
      formId: form._id,
      studentName: studentName.trim(),
      batch: batch.trim(),
      enrollmentNumber: enrollmentNumber.trim(),

      attendanceStatus,

      lowRatingReason:
        attendanceStatus === "Present" && lowRatingReason?.trim()
          ? lowRatingReason.trim()
          : null,

      answers: finalAnswers,
    });

    res.status(201).json({
      message:
        attendanceStatus === "Absent"
          ? "Absence recorded successfully"
          : "Feedback submitted successfully",

      responseId: response._id,
    });
  } catch (error) {
    console.error("SUBMIT FEEDBACK ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

module.exports = router;
