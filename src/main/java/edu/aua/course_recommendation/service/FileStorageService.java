package edu.aua.course_recommendation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String storeProfilePicture(MultipartFile file, UUID userId) throws IOException {
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "profile-pictures");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Create unique filename based on userId
        String filename = userId.toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Save the file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return the URL to access the file
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/profile-pictures/")
                .path(filename)
                .toUriString();
    }
}