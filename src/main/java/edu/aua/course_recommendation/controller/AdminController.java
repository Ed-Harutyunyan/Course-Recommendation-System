package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.service.auth.UserService;
import edu.aua.course_recommendation.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JwtService jwtService;
    private final UserService userService;

//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/get-token")
//    public ResponseEntity<String> getServiceToken(@RequestParam String serviceName) {
//        String token = jwtService.generateToken(serviceName, String.valueOf(Role.ROLE_SERVICE));
//        return ResponseEntity.ok(token);
//    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> helloWorld() {
        return ResponseEntity.ok("Hello World!");
    }

    @PostMapping("/user/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivateUser(@RequestParam String email) {
        userService.deactivateUser(email);
        return ResponseEntity.noContent().build();
    }


}
