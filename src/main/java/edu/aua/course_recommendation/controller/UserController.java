package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.UserProfileDto;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.exceptions.UserNotFoundException;
import edu.aua.course_recommendation.mappers.UserMapper;
import edu.aua.course_recommendation.repository.UserRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found"));
        return ResponseEntity.ok(userMapper.toUserProfileDto(user));
    }

}
