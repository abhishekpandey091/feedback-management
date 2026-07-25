const express = require("express");
const Form = require("../models/Form");
const User = require("../models/User");
const protect = require("../middleware/authMiddleware");

const router = express.Router();

router.post("/", protect, async (req, res) => {
  try {
    const { title, description, assignedTo, questions, allowedBatches } =
      req.body;

    // Basic validation
    if (!title || !questions || questions.length === 0) {
      return res.status(400).json({
        message: "Title and at least one question are required",
      });
    }

    // Only admin or teacher can create forms
    if (!["admin", "teacher"].includes(req.user.role)) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    // Admin must assign the form to a teacher
    if (req.user.role === "admin") {
      if (!assignedTo) {
        return res.status(400).json({
          message: "Please assign the form to a teacher",
        });
      }

      const teacher = await User.findOne({
        _id: assignedTo,
        role: "teacher",
        isActive: true,
      });

      if (!teacher) {
        return res.status(404).json({
          message: "Active teacher not found",
        });
      }
    }

    const form = await Form.create({
      title,
      description: description || "",
      createdBy: req.user.userId,

      assignedTo: req.user.role === "admin" ? assignedTo : req.user.userId,

      questions,
      allowedBatches: allowedBatches || [],

      // Every new form starts inactive
      isActive: false,
      activatedAt: null,

      // Admin forms are approved immediately.
      // Teacher forms require Admin approval.
      approvalStatus: req.user.role === "admin" ? "approved" : "pending",

      createdByRole: req.user.role,

      approvedBy: req.user.role === "admin" ? req.user.userId : null,

      approvedAt: req.user.role === "admin" ? new Date() : null,
    });

    res.status(201).json({
      message: "Form created successfully",
      form,
    });
  } catch (error) {
    console.error("CREATE FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// Get forms assigned to logged-in teacher
router.get("/my-forms", protect, async (req, res) => {
  try {
    if (req.user.role !== "teacher") {
      return res.status(403).json({
        message: "Access denied. Teacher only.",
      });
    }

    const forms = await Form.find({
      assignedTo: req.user.userId,
    }).sort({ createdAt: -1 });

    res.status(200).json({
      message: "Forms fetched successfully",
      forms,
    });
  } catch (error) {
    console.error("GET TEACHER FORMS ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// Admin approves a teacher-created form
router.patch("/:formId/approve", protect, async (req, res) => {
  try {
    // Only admin can approve
    if (req.user.role !== "admin") {
      return res.status(403).json({
        message: "Access denied. Admin only.",
      });
    }

    const form = await Form.findById(req.params.formId);

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    if (form.approvalStatus === "approved") {
      return res.status(400).json({
        message: "Form is already approved",
      });
    }

    form.approvalStatus = "approved";
    form.approvedBy = req.user.userId;
    form.approvedAt = new Date();

    await form.save();

    res.status(200).json({
      message: "Form approved successfully",
      form,
    });
  } catch (error) {
    console.error("APPROVE FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// Activate an approved form
router.patch("/:formId/activate", protect, async (req, res) => {
  try {
    if (req.user.role !== "admin") {
      return res.status(403).json({
        message: "Access denied. Admin only.",
      });
    }

    const form = await Form.findById(req.params.formId);

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    if (form.approvalStatus !== "approved") {
      return res.status(400).json({
        message: "Form must be approved before activation",
      });
    }

    if (form.isActive) {
      return res.status(400).json({
        message: "Form is already active",
      });
    }

    form.isActive = true;
    form.activatedAt = new Date();

    await form.save();

    res.status(200).json({
      message: "Form activated successfully",
      form,
    });
  } catch (error) {
    console.error("ACTIVATE FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// Get all forms available to logged-in user
router.get("/", protect, async (req, res) => {
  try {
    let forms;

    if (req.user.role === "admin") {
      // Admin can see every form
      forms = await Form.find()
        .populate("createdBy", "fullName email")
        .populate("assignedTo", "fullName email")
        .sort({ createdAt: -1 });
    } else if (req.user.role === "teacher") {
      // Teacher sees forms created by or assigned to them
      forms = await Form.find({
        $or: [{ createdBy: req.user.userId }, { assignedTo: req.user.userId }],
      })
        .populate("createdBy", "fullName email")
        .populate("assignedTo", "fullName email")
        .sort({ createdAt: -1 });
    } else {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    res.status(200).json({
      message: "Forms fetched successfully",
      forms,
    });
  } catch (error) {
    console.error("GET FORMS ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});
// Get a single form by ID
router.get("/:formId", protect, async (req, res) => {
  try {
    const form = await Form.findById(req.params.formId)
      .populate("createdBy", "fullName email")
      .populate("assignedTo", "fullName email");

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    // Teacher can only view forms created by or assigned to them
    if (req.user.role === "teacher") {
      const userId = req.user.userId;

      const isCreator = form.createdBy._id.toString() === userId;

      const isAssigned =
        form.assignedTo && form.assignedTo._id.toString() === userId;

      if (!isCreator && !isAssigned) {
        return res.status(403).json({
          message: "Access denied",
        });
      }
    }

    res.status(200).json({
      message: "Form fetched successfully",
      form,
    });
  } catch (error) {
    console.error("GET FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

// Get a single form by ID
router.get("/:formId", protect, async (req, res) => {
  try {
    const form = await Form.findById(req.params.formId)
      .populate("createdBy", "fullName email")
      .populate("assignedTo", "fullName email");

    if (!form) {
      return res.status(404).json({
        message: "Form not found",
      });
    }

    // Teacher can only view forms created by or assigned to them
    if (req.user.role === "teacher") {
      const userId = req.user.userId;

      const isCreator = form.createdBy._id.toString() === userId;

      const isAssigned =
        form.assignedTo && form.assignedTo._id.toString() === userId;

      if (!isCreator && !isAssigned) {
        return res.status(403).json({
          message: "Access denied",
        });
      }
    }

    res.status(200).json({
      message: "Form fetched successfully",
      form,
    });
  } catch (error) {
    console.error("GET FORM ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});
module.exports = router;
