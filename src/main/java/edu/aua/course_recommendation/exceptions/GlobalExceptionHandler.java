package edu.aua.course_recommendation.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseOfferingAlreadyExistsException.class)
    public ResponseEntity<String> handleCourseOfferingAlreadyExistsException(CourseOfferingAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(CourseAlreadyExistsException.class)
    public ResponseEntity<String> handleCourseAlreadyExistsException(CourseAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // TODO: Add other exceptions here as well.
}
