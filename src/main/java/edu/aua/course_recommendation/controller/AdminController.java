package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.model.Role;
import edu.aua.course_recommendation.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JwtService jwtService;

    public AdminController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-token")
    public ResponseEntity<String> getServiceToken(@RequestParam String serviceName) {
        String token = jwtService.generateToken(serviceName, String.valueOf(Role.ROLE_SERVICE));
        return ResponseEntity.ok(token);
    }
}
