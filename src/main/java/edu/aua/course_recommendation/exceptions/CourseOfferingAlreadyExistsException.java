package edu.aua.course_recommendation.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CourseOfferingAlreadyExistsException extends RuntimeException {
    public CourseOfferingAlreadyExistsException(String courseCode, String year, String semester) {
        super(String.format("Course offering already exists for code=%s, year=%s, semester=%s",
                courseCode, year, semester));
    }

    public CourseOfferingAlreadyExistsException(String message) {
        super(message);
    }
}