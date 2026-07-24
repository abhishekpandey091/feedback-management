const mongoose = require("mongoose");
const bcrypt = require("bcrypt");
const User = require("./models/User");

async function updateAdminPassword() {
  try {
    await mongoose.connect("mongodb://127.0.0.1:27017/feedback_app");

    console.log("MongoDB connected");

    const hashedPassword = await bcrypt.hash("admin123", 10);

    await User.updateOne(
      { email: "admin@example.com" },
      { password: hashedPassword },
    );

    console.log("Admin password hashed successfully");

    await mongoose.connection.close();
  } catch (error) {
    console.log("Error:", error);
  }
}

updateAdminPassword();
