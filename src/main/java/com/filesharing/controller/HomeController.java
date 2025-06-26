package com.filesharing.controller;

import com.filesharing.model.User;
import com.filesharing.service.UserService;
import com.filesharing.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "landing";
    }

    @GetMapping("/landing")
    public String landing() {
        return "landing";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        com.filesharing.model.User user = userService.findByEmail(userDetails.getUsername())
            .orElseGet(() -> userService.getUserRepository().findByUsername(userDetails.getUsername()).orElse(null));
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        // Add recent files (limit 5)
        java.util.List<com.filesharing.model.FileRecord> recentFiles = fileService.findByUser(user);
        if (recentFiles.size() > 5) recentFiles = recentFiles.subList(0, 5);
        model.addAttribute("recentFiles", recentFiles);
        model.addAttribute("uploadRequest", new com.filesharing.dto.FileUploadRequest());
        return "dashboard";
    }

    @GetMapping("/error")
    public String error(@RequestParam(value = "error", required = false) String error, Model model) {
        logger.warn("Accessing error page with error: {}", error);
        if (error != null) {
            model.addAttribute("error", error);
        } else {
            model.addAttribute("error", "You don't have authorization to view this page.");
        }
        return "error";
    }

    @GetMapping("/login")
    public String login() {
        logger.info("Accessing login page");
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        logger.info("Accessing signup page");
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@ModelAttribute User user, Model model) {
        logger.info("Processing signup for email: {}", user.getEmail());
        if (user.getEmail() == null || user.getEmail().isBlank() || user.getPassword() == null || user.getPassword().isBlank()) {
            logger.warn("Signup failed: missing email or password");
            model.addAttribute("error", "Email and password are required.");
            model.addAttribute("user", user);
            return "signup";
        }
        if (userService.existsByEmail(user.getEmail())) {
            logger.warn("Signup failed: user with email {} already exists", user.getEmail());
            model.addAttribute("error", "A user with this email already exists.");
            model.addAttribute("user", user);
            return "signup";
        }
        try {
            userService.saveUser(user);
            logger.info("User created successfully: {}", user.getEmail());
            model.addAttribute("success", "Account created successfully! Please sign in.");
            return "login";
        } catch (Exception e) {
            logger.error("Error creating account for {}: {}", user.getEmail(), e.getMessage(), e);
            model.addAttribute("error", "Error creating account: " + e.getMessage());
            model.addAttribute("user", user);
            return "signup";
        }
    }

    @GetMapping("/user/details")
    public String userDetails(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "5") int size,
                             Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        // Find user by email or username
        com.filesharing.model.User user = userService.findByEmail(userDetails.getUsername())
            .orElseGet(() -> userService.getUserRepository().findByUsername(userDetails.getUsername()).orElse(null));
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        Pageable pageable = PageRequest.of(page, size);
        Page<com.filesharing.model.FileRecord> filePage = fileService.findByUserPaginated(user, pageable);
        model.addAttribute("filePage", filePage);
        return "user-details";
    }
} 