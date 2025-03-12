package edu.aua.course_recommendation.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.util.Map;

public class EmailVerificationException extends ErrorResponseException {

    public EmailVerificationException(final HttpStatusCode status, final Map<String, String> errors) {
        super(status, createProblemDetail(status, errors), null);
    }

    private static ProblemDetail createProblemDetail(HttpStatusCode status, Map<String, String> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Email verification failed");
        problemDetail.setProperty("errors", errors); // Attach error details
        return problemDetail;
    }
}
