package uk.gov.pay.adminusers.resources;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import uk.gov.pay.adminusers.web.JsonMediaTypes;
import org.springframework.http.HttpStatus;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.apache.commons.lang3.StringUtils.defaultString;

@RestController
public class HealthCheckResource {
    private final HealthCheckRegistry healthCheckRegistry;

    public HealthCheckResource(HealthCheckRegistry healthCheckRegistry) {
        this.healthCheckRegistry = healthCheckRegistry;
    }

    @GetMapping("/healthcheck")
    @JsonMediaTypes
    @Operation(
            tags = "Other",
            summary = "Healthcheck endpoint for adminusers. Check database, and deadlocks",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(example = "{" +
                            "    \"database\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }," +
                            "    \"deadlocks\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }" +
                            "}")), description = "OK"),
                    @ApiResponse(responseCode = "503", description = "Service unavailable. If any healthchecks fail")
            }
    )
    public ResponseEntity<?> healthCheck() {
        SortedMap<String, HealthCheck.Result> results = healthCheckRegistry.runHealthChecks();

        Map<String, Map<String, Object>> response = results.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        healthcheckResult -> Map.of(
                                "healthy", healthcheckResult.getValue().isHealthy(),
                                "message", defaultString(healthcheckResult.getValue().getMessage(), "Healthy")
                        )
                ));

        HttpStatus status = allHealthy(results.values()) ? OK : SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(response);
    }

    private boolean allHealthy(Collection<HealthCheck.Result> results) {
        return results.stream().allMatch(HealthCheck.Result::isHealthy);
    }
}
