package com.autopulse.routingservice.service.client;

import com.autopulse.routingservice.vroom.dto.request.VroomRequest;
import com.autopulse.routingservice.vroom.dto.response.VroomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class VroomClient {

    private final ObjectMapper objectMapper;

    @Value("${app.vroom.request-output-dir}")
    private String vroomRequestOutputDir;

    @Value("${app.vroom.base-url}")
    private String vroomBaseUrl;

    public Path saveRequest(VroomRequest request) {
        return writePayload("vroom-request-", request, "Failed to write VROOM request to file");
    }

    public Path saveResponse(VroomResponse response) {
        return writePayload("vroom-response-", response, "Failed to write VROOM response to file");
    }

    public VroomResponse call(VroomRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<VroomResponse> response = restTemplate.exchange(vroomBaseUrl, HttpMethod.POST, new HttpEntity<>(request, headers), VroomResponse.class);

            VroomResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("VROOM returned empty response");
            }
            if (body.code() != null && body.code() != 0) {
                throw new IllegalStateException("VROOM returned error code: " + body.code());
            }

            return body;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call VROOM", e);
        }
    }

    private Path writePayload(String filePrefix, Object payload, String errorMessage) {
        try {
            String fileName = filePrefix + System.currentTimeMillis() + ".json";
            Path outputDir = Path.of(vroomRequestOutputDir).toAbsolutePath().normalize();
            Path filePath = outputDir.resolve(fileName);
            Files.createDirectories(outputDir);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), payload);
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException(errorMessage, e);
        }
    }
}
