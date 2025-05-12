package edu.aua.course_recommendation.service.schedule;

import edu.aua.course_recommendation.dto.request.MessageAndPossibleCourseDto;
import edu.aua.course_recommendation.dto.request.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.dto.response.CourseDto;
import edu.aua.course_recommendation.dto.response.CourseResponseDto;
import edu.aua.course_recommendation.dto.response.RecommendationDto;
import edu.aua.course_recommendation.exceptions.RecommendationException;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PythonService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CourseMapper courseMapper;
    private final CourseService courseService;
    @Value("${python.service.url}")
    private String pythonServiceEndpoint;

    public String sendCourses() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseResponseDto>> request =
                new HttpEntity<>(courseService
                        .getAllCourses()
                        .stream()
                        .map(courseMapper::toCourseResponseDto)
                        .toList(), headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "/api/vectorize",
                HttpMethod.PUT, request, String.class);

        return response.getBody();
    }

//    /** For freshman: keywords
//     *
//     * @param body List of keywords that interest the student and possible course ids
//     */
//    public ResponseEntity<String> sendKeywordsRecommendations(KeywordAndPossibleIdsDto body) {
//        HttpHeaders headers = new HttpHeaders();
//        String URL = pythonServiceEndpoint + "/api/recommend/keyword";
//
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<KeywordAndPossibleIdsDto> request = new HttpEntity<>(body, headers);
//
//        try {
//            ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
//                    URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
//            );
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                saveRecommendationsToFile(response.getBody());
//                return ResponseEntity.ok("JSON received and saved successfully in: " + dataPath);
//            } else {
//                return ResponseEntity.status(response.getStatusCode()).body("Error while fetching recommendations");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send keywords to python");
//        }
//    }

//    public ResponseEntity<String> getRecommendationsWithPassedCourses(List<UUID> passed_ids, List<UUID> possible_ids) {
//
//        PassedAndPossibleCoursesDto UUIDs = PassedAndPossibleCoursesDto.builder()
//                .passed_ids(passed_ids)
//                .possible_ids(possible_ids)
//                .build();
//
//        HttpHeaders headers = new HttpHeaders();
//        String URL = pythonServiceEndpoint + "/api/recommend/byPassed";
//
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<PassedAndPossibleCoursesDto> request = new HttpEntity<>(UUIDs, headers);
//
//        try {
//            ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
//                    URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
//            );
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                saveRecommendationsToFile(response.getBody());
//                return ResponseEntity.ok("JSON received and saved successfully in: " + dataPath);
//            } else {
//                return ResponseEntity.status(response.getStatusCode()).body("Error while fetching recommendations");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send keywords to python");
//        }
//    }

    public List<RecommendationDto> sendMessageRecommendations(MessageAndPossibleCourseDto body) {
        HttpHeaders headers = new HttpHeaders();
        String URL = pythonServiceEndpoint + "/api/recommend/message";

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MessageAndPossibleCourseDto> request = new HttpEntity<>(body, headers);

        ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
                URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RecommendationException("Failed to fetch recommendations from Python with status code: "
                    + response.getStatusCode());
        }
        return response.getBody();
    }

    public List<RecommendationDto> getRecommendationsWithPassedCourses(List<String> passed_ids, List<String> possible_ids) {
        PassedAndPossibleCoursesDto courseCodes = PassedAndPossibleCoursesDto.builder()
                                                                       .passed_course_codes(passed_ids)
                                                                       .possible_course_codes(possible_ids)
                                                                       .build();

        HttpHeaders headers = new HttpHeaders();
        String URL = pythonServiceEndpoint + "/api/recommend/byPassed";

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PassedAndPossibleCoursesDto> request = new HttpEntity<>(courseCodes, headers);

        ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
                URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode() != HttpStatus.OK && response.getBody() == null) {
            throw new RecommendationException("Failed to fetch recommendations from Python with status code: "
                    + response.getStatusCode());
        }
        return response.getBody();
    }

    /**
     * Use: Testing connection with Python ONLY
     *
     * @param data List of CourseDto
     * @return Response by the Python Server
     */
    public String sendTest(List<CourseDto> data) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseDto>> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/test",
                HttpMethod.POST, request, String.class);

        return response.getBody();
    }

    /*
    private <T> void saveRecommendationsToFile(List<T> body) {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(String.format(dataPath, "received_payload.json"));

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            objectMapper.writeValue(file, body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     */

}
