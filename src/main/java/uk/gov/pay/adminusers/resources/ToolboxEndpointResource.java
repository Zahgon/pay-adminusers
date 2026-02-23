package uk.gov.pay.adminusers.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.ServiceUserRemover;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import uk.gov.pay.adminusers.web.JsonMediaTypes;


import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.adminusers.utils.LoggingKeys.SERVICE_EXTERNAL_ID;
import static uk.gov.pay.adminusers.utils.LoggingKeys.USER_EXTERNAL_ID;

@RestController
public class ToolboxEndpointResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolboxEndpointResource.class);

    private final ServiceUserRemover serviceUserRemover;
    public ToolboxEndpointResource(ServiceUserRemover serviceUserRemover) {
        this.serviceUserRemover = serviceUserRemover;
    }

    @DeleteMapping("/v1/api/toolbox/services/{serviceExternalId}/users/{userExternalId}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Toolbox",
            summary = "Remove user from service (Toolbox use only)",
            operationId = "removeUserFromServiceUsingToolbox",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Delete user from service"),
                    @ApiResponse(responseCode = "404", description = "User or Service not found"),
            }
    )
    public ResponseEntity<?> removeUserFromService(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                          @PathVariable("serviceExternalId") String serviceExternalId,
                                          @Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                          @PathVariable("userExternalId") String userExternalId) {
        LOGGER.info(format("Toolbox DELETE request - removing user %s from service %s,", serviceExternalId, userExternalId),
                kv(SERVICE_EXTERNAL_ID, serviceExternalId),
                kv(USER_EXTERNAL_ID, userExternalId));
        serviceUserRemover.removeWithoutAdminCheck(userExternalId, serviceExternalId);
        LOGGER.info(format("Succeeded toolbox users DELETE request - user %s removed from service %s", serviceExternalId, userExternalId),
                kv(SERVICE_EXTERNAL_ID, serviceExternalId),
                kv(USER_EXTERNAL_ID, userExternalId));
        return ResponseEntity.status(NO_CONTENT).build();
    }
}
