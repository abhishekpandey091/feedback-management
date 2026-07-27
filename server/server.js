require("dotenv").config();

const express = require("express");
const mongoose = require("mongoose");
const authRoutes = require("./routes/authRoutes");
const protect = require("./middleware/authMiddleware");
const teacherRoutes = require("./routes/teacherRoutes");
const formRoutes = require("./routes/formRoutes");
const studentRoutes = require("./routes/studentRoutes");

const app = express();

mongoose
  .connect("mongodb://127.0.0.1:27017/feedback_app")
  .then(() => {
    console.log("MongoDB connected");
  })
  .catch((error) => {
    console.log("MongoDB connection failed:", error);
  });

app.use(express.json());

app.use("/api/auth", authRoutes);
app.use("/api/teachers", teacherRoutes);
app.use("/api/forms", formRoutes);
app.use("/api/public", studentRoutes);

app.get("/api/test", (req, res) => {
  res.json({
    message: "Backend is working",
  });
});

app.get("/api/protected", protect, (req, res) => {
  res.json({
    message: "You accessed a protected route",
    user: req.user,
  });
});

app.listen(5000, () => {
  console.log("Server running on port 5000");
});
