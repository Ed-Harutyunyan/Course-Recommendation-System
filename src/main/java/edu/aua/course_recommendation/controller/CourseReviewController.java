package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.request.CourseReviewRequestDto;
import edu.aua.course_recommendation.dto.response.CourseReviewResponseDto;
import edu.aua.course_recommendation.service.course.CourseReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course-reviews")
public class CourseReviewController {
    private final CourseReviewService reviewService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseReviewResponseDto>> getReviewsForCourse(@PathVariable UUID courseId) {
        List<CourseReviewResponseDto> reviews = reviewService.getReviewsForCourse(courseId);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping
    public ResponseEntity<CourseReviewResponseDto> createReview(
            @RequestParam UUID userId,
            @RequestBody CourseReviewRequestDto dto) {
        CourseReviewResponseDto reviewDto = reviewService.createReview(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewDto);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}