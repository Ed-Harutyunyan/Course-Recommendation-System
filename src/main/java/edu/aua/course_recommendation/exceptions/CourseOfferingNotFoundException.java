package edu.aua.course_recommendation.exceptions;

public class CourseOfferingNotFoundException extends RuntimeException {
    public CourseOfferingNotFoundException(String message) {
        super(message);
    }
}
