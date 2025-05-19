package edu.aua.course_recommendation.service.course;

import edu.aua.course_recommendation.dto.request.CourseReviewRequestDto;
import edu.aua.course_recommendation.dto.response.CourseReviewResponseDto;
import edu.aua.course_recommendation.entity.CourseReview;
import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.exceptions.AuthenticationException;
import edu.aua.course_recommendation.exceptions.ReviewNotFoundException;
import edu.aua.course_recommendation.repository.CourseReviewRepository;
import edu.aua.course_recommendation.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseReviewService {
    private final CourseReviewRepository reviewRepository;
    private final UserService userService;
    private final CourseService courseService;

    public List<CourseReviewResponseDto> getReviewsForCourse(UUID courseId) {
        List<CourseReview> reviews = reviewRepository.findByCourseId(courseId);
        return reviews.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public CourseReviewResponseDto createReview(UUID userId, CourseReviewRequestDto dto) {
        validateReviewInput(dto);

        CourseReview review = CourseReview.builder()
                .userId(userId)
                .courseId(dto.courseId())
                .content(dto.content().trim())
                .rating(dto.rating())
                .createdAt(LocalDateTime.now())
                .build();

        CourseReview savedReview = reviewRepository.save(review);
        return mapToResponseDto(savedReview);
    }

    private void validateReviewInput(CourseReviewRequestDto dto) {
        if (dto.rating() < 1 || dto.rating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (dto.content() == null || dto.content().trim().isEmpty()) {
            throw new IllegalArgumentException("Review content cannot be empty");
        }
        if (dto.content().length() > 1000) {
            throw new IllegalArgumentException("Review content cannot exceed 1000 characters");
        }
        if (dto.courseId() == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new AuthenticationException("Authentication required");
        }

        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (!review.getUserId().equals(currentUser.getId())) {
            throw new AuthenticationException("You can only delete your own reviews");
        }

        reviewRepository.deleteById(reviewId);
    }

    private CourseReviewResponseDto mapToResponseDto(CourseReview review) {
        String profilePictureUrl = userService.getProfilePictureUrl(review.getUserId());
        String userName = userService.getUserName(review.getUserId());
        String courseCode = courseService.getCourseCodeById(review.getCourseId());

        return new CourseReviewResponseDto(
                review.getId(),
                review.getUserId(),
                userName,
                review.getCourseId(),
                courseCode,
                review.getContent(),
                review.getRating(),
                review.getCreatedAt(),
                profilePictureUrl
        );
    }
}