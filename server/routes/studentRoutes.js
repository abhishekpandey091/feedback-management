const express = require("express");
const Form = require("../models/Form");
const Response = require("../models/Response");

const router = express.Router();

// Public: Get an active feedback form
router.get("/forms/:formId", async (req, res) => {
  try {
    const form = await Form.findOne({
      _id: req.params.formId,
      isActive: true,
      approvalStatus: "approved",
    }).select("title description questions allowedBatches assignedTo");

    if (!form) {
      return res.status(404).json({
        message: "Active form not found",
      });
    }

    res.status(200).json({
      message: "Form fetched successfully",
      form,
    });
  } catch (error) {
    console.error("PUBLIC FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// Public: Submit feedback
router.post("/forms/:formId/responses", async (req, res) => {
  try {
    const { studentName, batch, enrollmentNumber, answers } = req.body;

    if (
      !studentName ||
      !batch ||
      !enrollmentNumber ||
      !Array.isArray(answers)
    ) {
      return res.status(400).json({
        message: "Student details and answers are required",
      });
    }

    const form = await Form.findOne({
      _id: req.params.formId,
      isActive: true,
      approvalStatus: "approved",
    });

    if (!form) {
      return res.status(404).json({
        message: "Active form not found",
      });
    }

    // Validate batch when form has restricted batches
    if (
      form.allowedBatches.length > 0 &&
      !form.allowedBatches.includes(batch)
    ) {
      return res.status(403).json({
        message: "Your batch is not allowed for this form",
      });
    }

    // Validate required questions
    for (const question of form.questions) {
      if (!question.required) continue;

      const submittedAnswer = answers.find(
        (item) => item.questionId?.toString() === question._id.toString(),
      );

      if (
        !submittedAnswer ||
        submittedAnswer.answer === "" ||
        submittedAnswer.answer === null ||
        submittedAnswer.answer === undefined
      ) {
        return res.status(400).json({
          message: `Answer required: ${question.questionText}`,
        });
      }
    }

    const response = await Response.create({
      formId: form._id,
      studentName,
      batch,
      enrollmentNumber,
      answers,
    });

    res.status(201).json({
      message: "Feedback submitted successfully",
      responseId: response._id,
    });
  } catch (error) {
    // Duplicate form + enrollment number
    if (error.code === 11000) {
      return res.status(409).json({
        message: "You have already submitted this form",
      });
    }

    console.error("SUBMIT FEEDBACK ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

module.exports = router;
