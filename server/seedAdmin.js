require("dotenv").config();

const mongoose = require("mongoose");
const bcrypt = require("bcrypt");
const User = require("./models/User");

async function seedAdmin() {
  try {
    await mongoose.connect(process.env.MONGO_URI);

    console.log("MongoDB connected");

    const hashedPassword = await bcrypt.hash("admin123", 10);

    const existingAdmin = await User.findOne({
      email: "admin@example.com",
    });

    if (existingAdmin) {
      existingAdmin.password = hashedPassword;
      existingAdmin.role = "admin";
      existingAdmin.isActive = true;

      await existingAdmin.save();

      console.log("Existing admin updated");
    } else {
      await User.create({
        fullName: "Admin",
        email: "admin@example.com",
        password: hashedPassword,
        role: "admin",
        isActive: true,
      });

      console.log("Admin created successfully");
    }

    await mongoose.connection.close();
    console.log("MongoDB connection closed");
  } catch (error) {
    console.error("Error:", error);
    await mongoose.connection.close();
  }
}

seedAdmin();
