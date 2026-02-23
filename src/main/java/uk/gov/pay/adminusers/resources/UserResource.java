package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.AssignServiceAndRoleRequest;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.ExistingUserOtpDispatcher;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.service.ServiceRoleCreator;
import uk.gov.pay.adminusers.service.ServiceRoleUpdater;
import uk.gov.pay.adminusers.service.UserCreator;

import jakarta.validation.Valid;
import uk.gov.pay.adminusers.exception.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.pay.adminusers.web.JsonMediaTypes;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static uk.gov.pay.adminusers.model.User.FIELD_EMAIL;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@RestController
public class UserResource {

    public static final String USERS_RESOURCE = "/v1/api/users";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);

    private static final Splitter COMMA_SEPARATOR = Splitter.on(',').trimResults();

    public static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";

    private final UserServices userServices;
    private final UserCreator userCreator;
    private final ServiceRoleUpdater serviceRoleUpdater;
    private final ServiceRoleCreator serviceRoleCreator;

    private final ExistingUserOtpDispatcher existingUserOtpDispatcher;

    private final UserRequestValidator validator;

    private final LinksBuilder linksBuilder;
    public UserResource(UserServices userServices, UserRequestValidator validator, UserCreator userCreator,
                        ServiceRoleUpdater serviceRoleUpdater, ServiceRoleCreator serviceRoleCreator,
                        ExistingUserOtpDispatcher existingUserOtpDispatcher, LinksBuilder linksBuilder) {
        this.userServices = userServices;
        this.validator = validator;
        this.userCreator = userCreator;
        this.serviceRoleUpdater = serviceRoleUpdater;
        this.serviceRoleCreator = serviceRoleCreator;
        this.existingUserOtpDispatcher = existingUserOtpDispatcher;
        this.linksBuilder = linksBuilder;
    }

    @PostMapping(USERS_RESOURCE + "/find")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Find user by email",
            requestBody = @RequestBody(
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{" +
                            "    \"email\": \"user@somegovernmentdept.gov.uk\"" +
                            "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid search params"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> findUser(@org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("User FIND request");
        return validator.validateFindRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> userServices.findUserByEmail(payload.get(FIELD_EMAIL).asText())
                        .map(user -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(user))
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
    }

    @GetMapping(USERS_RESOURCE + "/{userExternalId}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Find user by user external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> getUser(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                            @PathVariable("userExternalId") String externalId) {
        LOGGER.info("User GET request - [ {} ]", externalId);
        return userServices.findUserByExternalId(externalId)
                .map(userEntity -> {
                    User user = linksBuilder.decorate(userEntity.toUser());
                    return ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(user);
                })
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @PostMapping(USERS_RESOURCE + "/admin-emails-for-gateway-accounts")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Get admin user emails for given gateway account IDs",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{" +
                            "    \"gatewayAccountIds\": [\"1\",\"2\"]" +
                            "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(example = "{" +
                                    "    \"1\": [" +
                                    "        \"user@somegovernmentdept.gov.uk\"" +
                                    "    ]," +
                                    "    \"2\": [" +
                                    "        \"user2@somegovernmentdept.gov.uk\"" +
                                    "    ]" +
                                    "}"))),
                    @ApiResponse(responseCode = "422", description = "Invalid request")
            }
    )
    public Map<String, List<String>> getAdminUserEmailsForGatewayAccountIds(@org.springframework.web.bind.annotation.RequestBody(required = false) @Valid Map<String, List<String>> gatewayAccountIds) {
        return userServices.getAdminUserEmailsForGatewayAccountIds(gatewayAccountIds.get("gatewayAccountIds"));
    }

    @GetMapping(USERS_RESOURCE)
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Gets users with the associated external ids",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
            }
    )
    public ResponseEntity<?> getUsers(@Parameter(schema = @Schema(example = "93ba1ec4ed6a4238a59f16ad97b4fa12,1234"))
                             @RequestParam(value = "ids", required = false) String externalIds) {
        List<String> externalIdsList = COMMA_SEPARATOR.splitToList(externalIds);

        List<User> users = userServices.findUsersByExternalIds(externalIdsList);

        return ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(users);
    }

    @PostMapping(USERS_RESOURCE)
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Create new user",
            description = "This is only used by the Pay CLI for creating Users. Users are usually created by completing an invite",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "409", description = "User with email already exists"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> createUser(@Parameter(schema = @Schema(implementation = CreateUserRequest.class))
                               @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode node) {
        LOGGER.info("Attempting user create request");
        return validator.validateCreateRequest(node)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> {
                    String roleName = node.get(CreateUserRequest.FIELD_ROLE_NAME).asText();
                    String email = node.get(CreateUserRequest.FIELD_EMAIL).asText();
                    try {
                        User newUser = userCreator.doCreate(CreateUserRequest.from(node), roleName);
                        LOGGER.info("User created successfully [{}]", newUser.getExternalId());
                        return ResponseEntity.status(CREATED).contentType(APPLICATION_JSON).body(newUser);
                    } catch (Exception e) {
                        return handleCreateUserException(email, e);
                    }
                });
    }

    @PostMapping(USERS_RESOURCE + "/authenticate")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Authenticate a given email/password",
            requestBody = @RequestBody(
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{ \"email\": \"user@somegovernmentdept.gov.uk\"," +
                            "\"password\": \"a-password\"}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorised")
            }
    )
    public ResponseEntity<?> authenticate(@org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode node) {
        LOGGER.info("User authenticate request");
        return validator.validateAuthenticateRequest(node)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(400).body(errors))
                .orElseGet(() -> {
                    Optional<User> userOptional = userServices.authenticate(
                            node.get("email").asText(),
                            node.get("password").asText());

                    return userOptional
                            .<ResponseEntity<?>>map(user -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(user))
                            .orElseGet(() ->
                                    ResponseEntity.status(UNAUTHORIZED).contentType(APPLICATION_JSON).body(unauthorisedErrorMessage()));
                });
    }

    @PostMapping(USERS_RESOURCE + "/{userExternalId}/second-factor")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Send OTP via SMS for an existing user",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{ \"provisional\": false}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> sendOtpSms(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                               @PathVariable("userExternalId") String externalId, @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("User 2FA new passcode request");
        return validator.validateNewSecondFactorPasscodeRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> {
                    boolean changingSignInMethod = payload != null && payload.get("provisional") != null && payload.get("provisional").asBoolean();

                    return userServices.findUserByExternalId(externalId).map(userEntity -> {
                        if (changingSignInMethod) {
                            existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(userEntity);
                            return ResponseEntity.status(OK).contentType(APPLICATION_JSON).build();
                        }

                        existingUserOtpDispatcher.sendSignInOtp(userEntity);
                        return ResponseEntity.status(OK).contentType(APPLICATION_JSON).build();
                    }).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
                });
    }

    @PostMapping(USERS_RESOURCE + "/{userExternalId}/second-factor/authenticate")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Authenticate 2FA code",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(requiredProperties = {"code"}, example = "{ \"code\": 123456}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorised")
            }
    )
    public ResponseEntity<?> authenticateSecondFactor(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                             @PathVariable("userExternalId") String externalId, @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("User 2FA authenticate passcode request");
        return validator.validate2FAAuthRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> userServices.authenticateSecondFactor(externalId, payload.get("code").asInt())
                        .map(user -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(user))
                        .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED)));
    }

    @PostMapping(USERS_RESOURCE + "/{userExternalId}/second-factor/provision")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Create a new provisional OTP key for a user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "404", description = "Not found"),
            }
    )
    public ResponseEntity<?> newSecondFactorOtpKey(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                          @PathVariable("userExternalId") String externalId) {
        LOGGER.info("User 2FA provision new OTP key request");
        return userServices.provisionNewOtpKey(externalId)
                .map(user -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(user))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @PostMapping(USERS_RESOURCE + "/{userExternalId}/second-factor/activate")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Activate a new OTP key and method for a user",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(
                            requiredProperties = {"second_factor", "code"},
                            example = "{ \"code\": 123456, \"second_factor\": \"SMS\"}"
                    ))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorised")
            }
    )
    public ResponseEntity<?> activateSecondFactorOtpKey(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                               @PathVariable("userExternalId") String externalId, @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("User 2FA activate new OTP key request");
        return validator.validate2faActivateRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> {
                    int code = payload.get("code").asInt();
                    SecondFactorMethod secondFactor = SecondFactorMethod.valueOf(payload.get("second_factor").asText());
                    return userServices.activateNewOtpKey(externalId, secondFactor, code)
                            .map(user -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(user))
                            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
                });
    }

    @PostMapping(USERS_RESOURCE + "/{userExternalId}/reset-second-factor")
    @JsonMediaTypes
    @Operation(
            tags = "Users",
            summary = "Reset 2FA to SMS",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "404", description = "User with userExternalId not found")
            }
    )
    public ResponseEntity<?> resetSecondFactor(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                      @PathVariable("userExternalId") String externalId) {
        return userServices.resetSecondFactor(externalId)
                .map(user -> ResponseEntity.status(OK).body(user))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @PatchMapping(USERS_RESOURCE + "/{userExternalId}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Update user attributes",
            description = "Patch user attributes. <br>" +
                    "Supports patching (replace op) fields `disabled, email, features, telephone_number` and `append` for attrbute `sessionVersion`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "User with userExternalId not found")
            }
    )
    public ResponseEntity<?> updateUserAttribute(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                        @PathVariable("userExternalId") String externalId,
                                        @Parameter(schema = @Schema(implementation = PatchRequest.class))
                                        @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode node) {
        LOGGER.info("User update attribute attempt request");
        return validator.validatePatchRequest(node)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> userServices.patchUser(externalId, PatchRequest.from(node))
                        .map(user -> ResponseEntity.status(OK).body(user))
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
    }

    @PutMapping(USERS_RESOURCE + "/{userExternalId}/services/{serviceExternalId}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Update user's role for a service",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(
                            requiredProperties = {"service_external_id", "role_name"},
                            example = "{ \"role_name\":\"admin\"}"
                    ))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "409", description = "User does not belong to service")
            }
    )
    public ResponseEntity<?> updateServiceRole(@Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                      @PathVariable("userExternalId") String userExternalId,
                                      @Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                      @PathVariable("serviceExternalId") String serviceExternalId, @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("User update service role request");
        return validator.validateServiceRole(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> {
                    String roleName = payload.get(User.FIELD_ROLE_NAME).asText();
                    return serviceRoleUpdater.doUpdate(userExternalId, serviceExternalId, roleName)
                            .map(user -> ResponseEntity.status(OK).body(user))
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
                });
    }

    @PostMapping(USERS_RESOURCE + "/{userExternalId}/services")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Users",
            summary = "Assign a new service along with role to a user",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(
                            requiredProperties = {"service_external_id", "role_name"},
                            example = "{ \"service_external_id\":\"7d19aff33f8948deb97ed16b2912dcd3\"," +
                                    "\"role_name\":\"admin\"}"
                    ))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = User.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "User with userExternalId not found"),
                    @ApiResponse(responseCode = "409", description = "User already got access to service")
            }
    )
    public ResponseEntity<?> assignServiceAndRoleToUser(
            @Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12") @PathVariable("userExternalId") String userExternalId, 
            @org.springframework.web.bind.annotation.RequestBody(required = false) @Valid AssignServiceAndRoleRequest payload) {
        
        return serviceRoleCreator.doCreate(userExternalId, payload.serviceExternalId(), RoleName.fromName(payload.roleName()))
                .map(user -> ResponseEntity.status(OK).body(user))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    private ResponseEntity<?> handleCreateUserException(String email, Exception e) {
        if (e.getMessage().contains(CONSTRAINT_VIOLATION_MESSAGE)) {
            throw conflictingEmail(email);
        } else if (e instanceof WebApplicationException) {
            throw (WebApplicationException) e;
        } else {
            LOGGER.error("unknown database error during user creation for user [{}]", email, e);
            throw internalServerError("unable to create user at this moment");
        }
    }

    private Map<String, List<String>> unauthorisedErrorMessage() {
        return Map.of("errors", List.of("invalid username and/or password"));
    }
}
