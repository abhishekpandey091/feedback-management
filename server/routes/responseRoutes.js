const express = require("express");
const Response = require("../models/Response");
const Form = require("../models/Form");
const protect = require("../middleware/authMiddleware");

const router = express.Router();

// Check whether logged-in user can access this form
async function canAccessForm(user, form) {
  if (user.role === "admin") {
    return true;
  }

  if (user.role === "teacher") {
    const userId = user.userId.toString();

    const createdBy = form.createdBy?.toString();

    const assignedTo = form.assignedTo?.toString();

    return createdBy === userId || assignedTo === userId;
  }

  return false;
}

// ======================================================
// GET ALL RESPONSES FOR A FORM
// ======================================================

router.get("/form/:formId", protect, async (req, res) => {
  try {
    const form = await Form.findById(req.params.formId);

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    if (!(await canAccessForm(req.user, form))) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    const responses = await Response.find({
      formId: form._id,
    }).sort({ submittedAt: -1 });

    res.status(200).json({
      message: "Responses fetched successfully",
      totalResponses: responses.length,
      responses,
    });
  } catch (error) {
    console.error("GET RESPONSES ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// ======================================================
// SUMMARY / ANALYTICS
// ======================================================

router.get("/form/:formId/summary", protect, async (req, res) => {
  try {
    const form = await Form.findById(req.params.formId);

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    if (!(await canAccessForm(req.user, form))) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    const responses = await Response.find({
      formId: form._id,
    });

    const summary = form.questions.map((question) => {
      const questionAnswers = responses
        .map((response) =>
          response.answers.find(
            (answer) =>
              answer.questionId.toString() === question._id.toString(),
          ),
        )
        .filter(Boolean)
        .map((answer) => answer.answer);

      const result = {
        questionId: question._id,
        questionText: question.questionText,
        type: question.type,
        totalAnswers: questionAnswers.length,
      };

      // Star rating analytics
      if (question.type === "star_rating") {
        const ratings = questionAnswers
          .map(Number)
          .filter((value) => !Number.isNaN(value));

        result.average =
          ratings.length > 0
            ? ratings.reduce((a, b) => a + b, 0) / ratings.length
            : 0;

        result.lowerCount = ratings.filter((rating) => rating < 8).length;
      }

      // MCQ / Dropdown / Yes-No counts
      if (["mcq", "dropdown", "yes_no"].includes(question.type)) {
        const counts = {};

        questionAnswers.forEach((answer) => {
          counts[answer] = (counts[answer] || 0) + 1;
        });

        result.counts = counts;
      }

      // Checkbox counts
      if (question.type === "checkbox") {
        const counts = {};

        questionAnswers.forEach((answerArray) => {
          if (!Array.isArray(answerArray)) return;

          answerArray.forEach((answer) => {
            counts[answer] = (counts[answer] || 0) + 1;
          });
        });

        result.counts = counts;
      }

      return result;
    });

    res.status(200).json({
      message: "Summary fetched successfully",
      formId: form._id,
      title: form.title,
      totalResponses: responses.length,
      summary,
    });
  } catch (error) {
    console.error("SUMMARY ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// ======================================================
// LOWER FEEDBACK
// Star rating below 8
// ======================================================

router.get("/form/:formId/lower-feedback", protect, async (req, res) => {
  try {
    const form = await Form.findById(req.params.formId);

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    if (!(await canAccessForm(req.user, form))) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    const starQuestionIds = form.questions
      .filter((question) => question.type === "star_rating")
      .map((question) => question._id.toString());

    const responses = await Response.find({
      formId: form._id,
    });

    const lowerFeedback = responses.filter((response) =>
      response.answers.some((answer) => {
        return (
          starQuestionIds.includes(answer.questionId.toString()) &&
          Number(answer.answer) < 8
        );
      }),
    );

    res.status(200).json({
      message: "Lower feedback fetched successfully",
      total: lowerFeedback.length,
      responses: lowerFeedback,
    });
  } catch (error) {
    console.error("LOWER FEEDBACK ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// ======================================================
// INDIVIDUAL RESPONSE
// Keep this LAST so it doesn't swallow /form/... routes
// ======================================================

router.get("/:responseId", protect, async (req, res) => {
  try {
    const response = await Response.findById(req.params.responseId);

    if (!response) {
      return res.status(404).json({
        message: "Response not found",
      });
    }

    const form = await Form.findById(response.formId);

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    if (!(await canAccessForm(req.user, form))) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    res.status(200).json({
      message: "Response fetched successfully",
      response,
    });
  } catch (error) {
    console.error("INDIVIDUAL RESPONSE ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

module.exports = router;
