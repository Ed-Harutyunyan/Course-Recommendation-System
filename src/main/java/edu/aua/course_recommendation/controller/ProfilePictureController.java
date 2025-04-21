package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.service.FileStorageService;
import edu.aua.course_recommendation.service.auth.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/profile-picture")
public class ProfilePictureController {

    private final FileStorageService fileStorageService;
    private final UserService userService;

    public ProfilePictureController(FileStorageService fileStorageService, UserService userService) {
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }

            User currentUser = userService.getCurrentUser();

            // Store the file
            String fileUrl = fileStorageService.storeProfilePicture(file, currentUser.getId());

            // Update user profile with picture URL
            currentUser.setProfilePictureUrl(fileUrl);
            userService.saveUser(currentUser);

            Map<String, String> response = new HashMap<>();
            response.put("profilePictureUrl", fileUrl);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload profile picture: " + e.getMessage());
        }
    }
}