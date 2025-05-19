package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.UserRequestDto;
import edu.aua.course_recommendation.dto.response.UserProfileDto;
import edu.aua.course_recommendation.dto.response.UserResponseDto;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.exceptions.UserNotFoundException;
import edu.aua.course_recommendation.mappers.UserMapper;
import edu.aua.course_recommendation.repository.UserRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found"));
        return ResponseEntity.ok(userMapper.toUserProfileDto(user));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponseDto> userDtos = users.stream()
                .map(userMapper::toUserResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserRequestDto userDto) {
        User user = userService.createUser(userDto);
        UserResponseDto userResponseDto = userMapper.toUserResponseDto(user);
        return ResponseEntity.status(201).body(userResponseDto);
    }

    @DeleteMapping("/delete/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(java.util.UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

}
