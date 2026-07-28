const mongoose = require("mongoose");

const answerSchema = new mongoose.Schema(
  {
    questionId: {
      type: mongoose.Schema.Types.ObjectId,
      required: true,
    },

    answer: {
      type: mongoose.Schema.Types.Mixed,
      required: true,
    },
  },
  { _id: false },
);

const responseSchema = new mongoose.Schema(
  {
    formId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Form",
      required: true,
    },

    studentName: {
      type: String,
      required: true,
      trim: true,
    },

    batch: {
      type: String,
      required: true,
      trim: true,
    },

    enrollmentNumber: {
      type: String,
      required: true,
      trim: true,
    },

    attendanceStatus: {
      type: String,
      enum: ["Present", "Absent"],
      required: true,
    },

    lowRatingReason: {
      type: String,
      trim: true,
      default: null,
    },

    answers: {
      type: [answerSchema],
      required: true,
    },

    submittedAt: {
      type: Date,
      default: Date.now,
    },
  },
  {
    timestamps: true,
  },
);

module.exports = mongoose.model("Response", responseSchema);
