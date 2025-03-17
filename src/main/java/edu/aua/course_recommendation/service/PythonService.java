package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.dto.CourseDto;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class PythonService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String pythonServiceEndpoint;

    public PythonService (Environment env) {
        this.pythonServiceEndpoint = env.getProperty("python.service.url", "http://localhost:5000");
    }

    /**
     * Use: Testing connection with Python ONLY
     * @param data
     * @return
     */
    public String sendTest(List<CourseDto> data) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseDto>> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/test",
                HttpMethod.POST, request, String.class);

        return response.getBody();
    }

    public String sendCourse(List<CourseDto> data) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseDto>> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/vectorize",
                HttpMethod.PUT, request, String.class);

        return response.getBody();
    }
}
