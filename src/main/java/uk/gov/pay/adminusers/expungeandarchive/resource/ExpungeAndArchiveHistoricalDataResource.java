package uk.gov.pay.adminusers.expungeandarchive.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import uk.gov.pay.adminusers.expungeandarchive.service.ExpungeAndArchiveHistoricalDataService;

import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import uk.gov.pay.adminusers.web.JsonMediaTypes;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.gov.pay.adminusers.utils.LoggingKeys.MDC_REQUEST_ID_KEY;

@RestController
@Tag(name = "Tasks")
public class ExpungeAndArchiveHistoricalDataResource {

    private final ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService;
    public ExpungeAndArchiveHistoricalDataResource(ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService) {
        this.expungeAndArchiveHistoricalDataService = expungeAndArchiveHistoricalDataService;
    }
    @PostMapping("/v1/tasks/expunge-and-archive-historical-data")
    @JsonMediaTypes
    @Operation(
            summary = "Deletes and archives historical data based on `expungeAndArchiveDataConfig`. " +
                    "Currently,  " +
                    " 1. deletes historical users not attached to any service" +
                    " 2. deletes historical invites" +
                    " 3. deletes forgotten passwords" +
                    " 4. archives services without transactions within the configured number of days and detaches users from service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
            }
    )
    public ResponseEntity<?> expungeAndArchiveHistoricalData() {
        String correlationId = MDC.get(MDC_REQUEST_ID_KEY) == null ? "ExpungeAndArchiveHistoricalDataResource-" + UUID.randomUUID() : MDC.get(MDC_REQUEST_ID_KEY);
        MDC.put(MDC_REQUEST_ID_KEY, correlationId);

        expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

        MDC.remove(MDC_REQUEST_ID_KEY);
        return ResponseEntity.ok().build();
    }
}
