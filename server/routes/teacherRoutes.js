const express = require("express");
const bcrypt = require("bcrypt");
const User = require("../models/User");
const protect = require("../middleware/authMiddleware");
const adminOnly = require("../middleware/adminMiddleware");

const router = express.Router();

router.post("/", protect, adminOnly, async (req, res) => {
  try {
    const { fullName, email, password } = req.body;

    if (!fullName || !email || !password) {
      return res.status(400).json({
        message: "Full name, email and password are required",
      });
    }

    const existingUser = await User.findOne({ email });

    if (existingUser) {
      return res.status(400).json({
        message: "User with this email already exists",
      });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const teacher = await User.create({
      fullName,
      email,
      password: hashedPassword,
      role: "teacher",
      isActive: true,
    });

    res.status(201).json({
      message: "Teacher created successfully",
      teacher: {
        id: teacher._id,
        fullName: teacher.fullName,
        email: teacher.email,
        role: teacher.role,
        isActive: teacher.isActive,
      },
    });
  } catch (error) {
    res.status(500).json({
      message: "Server error",
    });
  }
});

router.get("/", protect, adminOnly, async (req, res) => {
  try {
    const teachers = await User.find({ role: "teacher" }).select("-password");

    res.status(200).json({
      message: "Teachers fetched successfully",
      teachers: teachers,
    });
  } catch (error) {
    res.status(500).json({
      message: "Server error",
    });
  }
});

router.put("/:id", protect, adminOnly, async (req, res) => {
  try {
    const { fullName, email } = req.body;

    const teacher = await User.findOne({
      _id: req.params.id,
      role: "teacher",
    });

    if (!teacher) {
      return res.status(404).json({
        message: "Teacher not found",
      });
    }

    if (email && email !== teacher.email) {
      const existingUser = await User.findOne({ email });

      if (existingUser) {
        return res.status(400).json({
          message: "User with this email already exists",
        });
      }
    }

    if (fullName) {
      teacher.fullName = fullName;
    }

    if (email) {
      teacher.email = email;
    }

    await teacher.save();

    res.status(200).json({
      message: "Teacher updated successfully",
      teacher: {
        id: teacher._id,
        fullName: teacher.fullName,
        email: teacher.email,
        role: teacher.role,
        isActive: teacher.isActive,
      },
    });
  } catch (error) {
    res.status(500).json({
      message: "Server error",
    });
  }
});

router.delete("/:id", protect, adminOnly, async (req, res) => {
  try {
    const teacher = await User.findOne({
      _id: req.params.id,
      role: "teacher",
    });

    if (!teacher) {
      return res.status(404).json({
        message: "Teacher not found",
      });
    }

    await teacher.deleteOne();

    res.status(200).json({
      message: "Teacher deleted successfully",
    });
  } catch (error) {
    res.status(500).json({
      message: "Server error",
    });
  }
});

router.patch("/:id/toggle-active", protect, adminOnly, async (req, res) => {
  try {
    const teacher = await User.findOne({
      _id: req.params.id,
      role: "teacher",
    });

    if (!teacher) {
      return res.status(404).json({
        message: "Teacher not found",
      });
    }

    teacher.isActive = !teacher.isActive;

    await teacher.save();

    res.status(200).json({
      message: `Teacher ${teacher.isActive ? "activated" : "deactivated"} successfully`,
      teacher: {
        id: teacher._id,
        fullName: teacher.fullName,
        email: teacher.email,
        role: teacher.role,
        isActive: teacher.isActive,
      },
    });
  } catch (error) {
    console.error("TOGGLE TEACHER ERROR:", error);

    res.status(500).json({
      message: "Server error",
    });
  }
});

module.exports = router;
