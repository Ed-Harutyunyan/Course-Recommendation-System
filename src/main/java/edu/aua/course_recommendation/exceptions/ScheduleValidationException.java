package edu.aua.course_recommendation.exceptions;

import lombok.Getter;

@Getter
public class ScheduleValidationException extends RuntimeException {
    private final ValidationError error;

    public ScheduleValidationException(ValidationError error, String message) {
        super(message);
        this.error = error;
    }

}

