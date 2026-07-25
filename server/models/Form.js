const mongoose = require("mongoose");

const questionSchema = new mongoose.Schema({
  questionText: {
    type: String,
    required: true,
    trim: true,
  },

  type: {
    type: String,
    required: true,
    enum: [
      "short",
      "paragraph",
      "mcq",
      "checkbox",
      "dropdown",
      "star_rating",
      "yes_no",
    ],
  },

  options: {
    type: [String],
    default: [],
  },

  maxStars: {
    type: Number,
    default: 10,
  },

  required: {
    type: Boolean,
    default: true,
  },
});

const formSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: true,
      trim: true,
    },

    description: {
      type: String,
      default: "",
    },

    createdBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    assignedTo: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },

    questions: {
      type: [questionSchema],
      default: [],
    },

    allowedBatches: {
      type: [String],
      default: [],
    },

    isActive: {
      type: Boolean,
      default: false,
    },

    activatedAt: {
      type: Date,
      default: null,
    },

    approvalStatus: {
      type: String,
      enum: ["approved", "pending", "rejected"],
      required: true,
    },

    createdByRole: {
      type: String,
      enum: ["admin", "teacher"],
      required: true,
    },

    approvedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },

    approvedAt: {
      type: Date,
      default: null,
    },

    rejectionReason: {
      type: String,
      default: "",
    },
  },
  {
    timestamps: true,
  },
);

module.exports = mongoose.model("Form", formSchema);
