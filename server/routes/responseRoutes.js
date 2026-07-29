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
// GET RESPONSE FOR RE-FEEDBACK
// ======================================================

router.get("/:responseId/refeedback", protect, async (req, res) => {
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
      message: "Re-feedback data fetched successfully",

      form: {
        _id: form._id,
        title: form.title,
        description: form.description,
        questions: form.questions,
      },

      response: {
        _id: response._id,
        studentName: response.studentName,
        enrollmentNumber: response.enrollmentNumber,
        batch: response.batch,
        attendanceStatus: response.attendanceStatus,
        lowRatingReason: response.lowRatingReason,
        answers: response.answers,
      },
    });
  } catch (error) {
    console.error("GET RE-FEEDBACK ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// ======================================================
// SUBMIT RE-FEEDBACK
// ======================================================

router.put("/:responseId/refeedback", protect, async (req, res) => {
  try {
    const { answers, lowRatingReason } = req.body;

    if (!Array.isArray(answers)) {
      return res.status(400).json({
        message: "Answers are required",
      });
    }

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

    // ---------------------------------------------
    // Validate every submitted answer
    // ---------------------------------------------

    let hasLowRating = false;

    for (const question of form.questions) {
      const oldAnswer = response.answers.find(
        (answer) => answer.questionId.toString() === question._id.toString(),
      );

      const newAnswer = answers.find(
        (answer) => answer.questionId?.toString() === question._id.toString(),
      );

      if (!newAnswer) {
        if (question.required) {
          return res.status(400).json({
            message: `Answer required: ${question.questionText}`,
          });
        }

        continue;
      }

      // ---------------------------------------------
      // YES -> NO IS NOT ALLOWED
      // ---------------------------------------------

      if (
        question.type === "yes_no" &&
        oldAnswer?.answer === "Yes" &&
        newAnswer.answer === "No"
      ) {
        return res.status(400).json({
          message: `Yes cannot be changed to No: ${question.questionText}`,
        });
      }

      // Yes/No validation
      if (
        question.type === "yes_no" &&
        !["Yes", "No"].includes(newAnswer.answer)
      ) {
        return res.status(400).json({
          message: `Invalid Yes/No answer: ${question.questionText}`,
        });
      }

      // MCQ / Dropdown
      if (
        ["mcq", "dropdown"].includes(question.type) &&
        !question.options.includes(newAnswer.answer)
      ) {
        return res.status(400).json({
          message: `Invalid answer for: ${question.questionText}`,
        });
      }

      // Checkbox
      if (question.type === "checkbox") {
        if (!Array.isArray(newAnswer.answer)) {
          return res.status(400).json({
            message: `Invalid checkbox answer: ${question.questionText}`,
          });
        }

        const invalidOption = newAnswer.answer.some(
          (option) => !question.options.includes(option),
        );

        if (invalidOption) {
          return res.status(400).json({
            message: `Invalid option for: ${question.questionText}`,
          });
        }
      }

      // Star rating
      if (question.type === "star_rating") {
        const rating = Number(newAnswer.answer);

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

    // Rating below 8 still requires reason
    if (hasLowRating && (!lowRatingReason || !lowRatingReason.trim())) {
      return res.status(400).json({
        message: "Reason is required for a rating below 8",
      });
    }

    response.answers = answers;

    response.lowRatingReason = lowRatingReason?.trim() || null;

    await response.save();

    res.status(200).json({
      message: "Re-feedback submitted successfully",
      response,
    });
  } catch (error) {
    console.error("SUBMIT RE-FEEDBACK ERROR:", error);

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
