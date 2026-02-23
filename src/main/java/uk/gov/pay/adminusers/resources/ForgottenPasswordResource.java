package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.service.ForgottenPasswordServices;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.Optional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.pay.adminusers.web.JsonMediaTypes;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RestController
@Tag(name = "Passwords")
public class ForgottenPasswordResource {

    public static final String FORGOTTEN_PASSWORDS_RESOURCE = "/v1/api/forgotten-passwords";
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgottenPasswordResource.class);

    private static final int MAX_LENGTH = 255;
    private final ForgottenPasswordServices forgottenPasswordServices;
    private final ForgottenPasswordValidator validator;

    public ForgottenPasswordResource(ForgottenPasswordServices forgottenPasswordServices,
                                     ForgottenPasswordValidator validator) {
        this.forgottenPasswordServices = forgottenPasswordServices;
        this.validator = validator;
    }

    @PostMapping(FORGOTTEN_PASSWORDS_RESOURCE)
    @JsonMediaTypes(consumes = true)
    @Operation(
            summary = "Create a new forgotten password request (sends email to user)",
            requestBody = @RequestBody(content = @Content(schema = @Schema(requiredProperties = {"username"},
                    example = "{" +
                            "    \"username\": \"a75f2719456c80eb29a10bbf1c92e7b7@example.com\"" +
                            "}"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> sendForgottenPassword(@org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("ForgottenPassword CREATE request");
        Optional<Errors> errorsOptional = validator.validateCreateRequest(payload);
        return errorsOptional
                .<ResponseEntity<?>>map(errors ->
                        ResponseEntity.status(BAD_REQUEST).contentType(APPLICATION_JSON).body(errors))
                .orElseGet(() -> {
                    forgottenPasswordServices.create(payload.get("username").asText());
                    return ResponseEntity.status(OK).build();
                });
    }

    @GetMapping(FORGOTTEN_PASSWORDS_RESOURCE + "/{code}")
    @JsonMediaTypes
    @Operation(
            summary = "Verify forgotten password code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = ForgottenPassword.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> findNonExpiredForgottenPassword(@Parameter(example = "bc9039e00cba4e63b2c92ecd0e188aba") @PathVariable("code") String code) {
        LOGGER.info("ForgottenPassword GET request");
        if (isNotBlank(code) && code.length() > MAX_LENGTH) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return forgottenPasswordServices.findNonExpired(code)
                .map(forgottenPassword -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(forgottenPassword))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
