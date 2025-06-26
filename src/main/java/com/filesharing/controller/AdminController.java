package com.filesharing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.filesharing.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.filesharing.service.FileService;
import org.springframework.data.domain.Pageable;

@Controller
public class AdminController {
    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        long totalUsers = userService.getAllUsers().size();
        FileService.FileStatistics stats = fileService.getFileStatistics();
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeFiles", stats.getActiveFiles());
        model.addAttribute("expiredFiles", stats.getExpiredFiles());
        model.addAttribute("limitReachedFiles", stats.getLimitReachedFiles());
        return "admin-dashboard";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "q", required = false) String query,
                       Model model) {
        Page<com.filesharing.model.User> userPage;
        if (query != null && !query.isBlank()) {
            userPage = userService.searchUsersByEmail(query, PageRequest.of(page, size));
        } else {
            userPage = userService.getAllUsersPaginated(PageRequest.of(page, size));
        }
        model.addAttribute("userPage", userPage);
        model.addAttribute("q", query);
        return "admin-users";
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/files")
    public String files(@RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "q", required = false) String query,
                       Model model) {
        org.springframework.data.domain.Page<com.filesharing.model.FileRecord> filePage;
        if (query != null && !query.isBlank()) {
            filePage = fileService.searchFilesByFilename(query, org.springframework.data.domain.PageRequest.of(page, size));
        } else {
            filePage = fileService.getAllFilesPaginated(org.springframework.data.domain.PageRequest.of(page, size));
        }
        model.addAttribute("filePage", filePage);
        model.addAttribute("q", query);
        return "admin-files";
    }

    @PostMapping("/admin/files/delete/{id}")
    public String deleteFile(@PathVariable Long id) {
        fileService.deleteFileRecord(id);
        return "redirect:/admin/files";
    }
} 