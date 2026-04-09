package com.taskmanager.controller;

import com.taskmanager.entity.User;
import com.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import com.taskmanager.service.UserService;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // show login page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // handle login
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        Model model) {

        User user = userRepository.findByEmail(email);

        if (user != null && user.getPassword().equals(password)) {
            return "redirect:/tasks"; // ✅ success
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login"; // ❌ stay here
        }
    }


    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }


    // 🔥 HANDLE SIGNUP
    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user) {

        // check if email exists
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return "redirect:/signup?error";
        }

        userRepository.save(user);

        return "redirect:/login";
    }

    @GetMapping("/google-login")
    public String googleLogin() {
        return "google-login"; // page name
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String newPassword,
                                Model model) {

        User user = userRepository.findByEmail(email);

        if (user != null) {
            user.setPassword(newPassword);
            userRepository.save(user);

            model.addAttribute("success", "Password updated successfully!");
        } else {
            model.addAttribute("error", "Email not found!");
        }

        return "reset-password";
    }



}