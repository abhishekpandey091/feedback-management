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

// Prevent same student submitting same form twice
responseSchema.index(
  {
    formId: 1,
    enrollmentNumber: 1,
  },
  {
    unique: true,
  },
);

module.exports = mongoose.model("Response", responseSchema);
