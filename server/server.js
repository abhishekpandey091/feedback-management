require("dotenv").config();

const express = require("express");
const mongoose = require("mongoose");

const authRoutes = require("./routes/authRoutes");
const protect = require("./middleware/authMiddleware");
const teacherRoutes = require("./routes/teacherRoutes");
const formRoutes = require("./routes/formRoutes");
const studentRoutes = require("./routes/studentRoutes");
const responseRoutes = require("./routes/responseRoutes");

const app = express();

app.use(express.json());

// Routes
app.use("/api/auth", authRoutes);
app.use("/api/teachers", teacherRoutes);
app.use("/api/public", studentRoutes);
app.use("/api/responses", responseRoutes);
app.use("/api/forms", formRoutes);

// Test route
app.get("/api/test", (req, res) => {
  res.json({
    message: "Backend is working",
  });
});

// Protected test route
app.get("/api/protected", protect, (req, res) => {
  res.json({
    message: "You accessed a protected route",
    user: req.user,
  });
});

// MongoDB
mongoose
  .connect(process.env.MONGO_URI)
  .then(() => {
    console.log("MongoDB connected");
  })
  .catch((error) => {
    console.error("MongoDB connection failed:", error);
  });

// Render and similar hosts provide PORT automatically
const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
