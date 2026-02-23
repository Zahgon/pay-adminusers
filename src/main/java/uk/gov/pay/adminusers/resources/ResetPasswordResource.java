package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.ResetPasswordService;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import uk.gov.pay.adminusers.web.JsonMediaTypes;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static uk.gov.pay.adminusers.utils.Errors.from;

@RestController
public class ResetPasswordResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordResource.class);
    private static final String RESET_PASSWORD_RESOURCE = "/v1/api/reset-password";
    /* default */ static final String FIELD_CODE = "forgotten_password_code";
    /* default */ static final String FIELD_PASSWORD = "new_password";

    private final ResetPasswordValidator resetPasswordValidator;
    private final ResetPasswordService resetPasswordService;
    public ResetPasswordResource(ResetPasswordValidator resetPasswordValidator, ResetPasswordService resetPasswordService) {
        this.resetPasswordValidator = resetPasswordValidator;
        this.resetPasswordService = resetPasswordService;
    }

    @PostMapping(RESET_PASSWORD_RESOURCE)
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = {"Passwords"},
            summary = "Reset password",
            requestBody = @RequestBody(content = @Content(schema = @Schema(requiredProperties = {"forgotten_password_code", "new_password"},
                    example = "{" +
                            "    \"forgotten_password_code\": \"bc9039e00cba4e63b2c92ecd0e188aba\"," +
                            "    \"new_password\": \"new-password\"" +
                            "}"))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Updated password successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Expired or non-existent code")
            }
    )
    public ResponseEntity<?> resetForgottenPassword(@org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {

        return resetPasswordValidator.validateResetRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST)
                        .contentType(APPLICATION_JSON)
                        .body(errors))
                .orElseGet(() -> resetPasswordService.updatePassword(payload.get(FIELD_CODE).asText(), payload.get(FIELD_PASSWORD).asText())
                        .map(userId -> {
                            LOGGER.info("user ID {} updated password successfully", userId);
                            return ResponseEntity.status(NO_CONTENT).build();
                        })
                        .orElseGet(() -> ResponseEntity.status(NOT_FOUND).body(from(List.of(String.format("Field [%s] non-existent/expired", FIELD_CODE))))));
    }
}
