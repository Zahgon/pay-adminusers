package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.CompleteInviteRequest;
import uk.gov.pay.adminusers.model.CompleteInviteResponse;
import uk.gov.pay.adminusers.model.CreateInviteToJoinServiceRequest;
import uk.gov.pay.adminusers.model.CreateSelfRegistrationInviteRequest;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.service.AdminUsersExceptions;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.InviteFinder;
import uk.gov.pay.adminusers.service.JoinServiceInviteCreator;
import uk.gov.pay.adminusers.service.SelfRegistrationInviteCreator;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import jakarta.validation.Valid;
import uk.gov.pay.adminusers.exception.WebApplicationException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.pay.adminusers.web.JsonMediaTypes;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.pay.adminusers.utils.email.EmailValidator.isPublicSectorEmail;

@RestController
@Tag(name = "Invites")
public class InviteResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteResource.class);
    private static final int MAX_LENGTH_CODE = 255;

    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;
    private final InviteFinder inviteFinder;
    private final SelfRegistrationInviteCreator selfRegistrationInviteCreator;
    private final JoinServiceInviteCreator joinServiceInviteCreator;

    public InviteResource(InviteService service, InviteRequestValidator inviteValidator, InviteFinder inviteFinder,
                          SelfRegistrationInviteCreator selfRegistrationInviteCreator,
                          JoinServiceInviteCreator joinServiceInviteCreator) {
        inviteService = service;
        this.inviteValidator = inviteValidator;
        this.inviteFinder = inviteFinder;
        this.selfRegistrationInviteCreator = selfRegistrationInviteCreator;
        this.joinServiceInviteCreator = joinServiceInviteCreator;
    }

    @GetMapping("/v1/api/invites/{code}")
    @JsonMediaTypes
    @Operation(
            summary = "Find invite for invite code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> getInvite(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathVariable("code") String code) {
        LOGGER.info("Invite GET request");
        if (isNotBlank(code) && code.length() > MAX_LENGTH_CODE) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return inviteFinder.find(code)
                .map(invite -> ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(invite))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }


    @PatchMapping("/v1/api/invites/{code}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            summary = "Update an invite",
            requestBody = @RequestBody(content = @Content(schema = @Schema(example = "[" +
                    "    {" +
                    "        \"op\": \"replace\"," +
                    "        \"path\": \"telephone_number\"," +
                    "        \"value\": \"+441134960000\"" +
                    "    }," +
                    "    {" +
                    "        \"op\": \"replace\"," +
                    "        \"path\": \"password\"," +
                    "        \"value\": \"a-password\"" +
                    "    }" +
                    "]"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "422", description = "Missing required fields or invalid values"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "404", description = "Invite not found")
            }
    )
    public Invite updateInvite(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathVariable("code") String inviteCode,
                               @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        inviteValidator.validatePatchRequest(payload);
        List<JsonPatchRequest> updateRequests = StreamSupport.stream(payload.spliterator(), false)
                .map(JsonPatchRequest::from)
                .collect(Collectors.toList());
        return inviteService.updateInvite(inviteCode, updateRequests);
    }
    
    @PostMapping("/v1/api/invites/{code}/send-otp")
    @JsonMediaTypes
    @Operation(
            summary = "Sends otp verification code to the phone number registered in the invite",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No content"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "412", description = "Precondition failed")
            }
    )
    public ResponseEntity<Void> sendOtp(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathVariable("code") String inviteCode) {
        LOGGER.info("Invite send OTP POST request");
        if (isNotBlank(inviteCode) && inviteCode.length() > MAX_LENGTH_CODE) {
            throw new WebApplicationException(NOT_FOUND);
        }
        inviteService.sendOtp(inviteCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/v1/api/invites/{code}/reprovision-otp")
    @JsonMediaTypes
    @Operation(
            summary = "Re-provision otp secret key for the invite",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Invite reprovisionOtp(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathVariable("code") String inviteCode) {
        LOGGER.info("Invite re-provision OTP POST request");
        if (isNotBlank(inviteCode) && inviteCode.length() > MAX_LENGTH_CODE) {
            throw new WebApplicationException(NOT_FOUND);
        }
        return inviteService.reprovisionOtp(inviteCode);
    }

    @PostMapping("/v1/api/invites/{code}/complete")
    @JsonMediaTypes
    @Operation(
            summary = "Completes the invite by creating user/service and invalidating the invite code",
            description = "In the case of a user invite, this resource will assign the new service to the existing user and disables the invite. <br>" +
                    "In the case of a service invite, this resource will create a new service, assign gateway account ids (if provided) and also creates a new user and assign to the service<br>" +
                    "The response contains the user and the service id's affected as part of the invite completion in addition to the invite",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = CompleteInviteResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public CompleteInviteResponse completeInvite(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathVariable("code") String inviteCode, @org.springframework.web.bind.annotation.RequestBody(required = false) @Valid CompleteInviteRequest completeInviteRequest) {
        LOGGER.info("Invite complete POST request");
        if (isNotBlank(inviteCode) && inviteCode.length() > MAX_LENGTH_CODE) {
            throw new WebApplicationException(NOT_FOUND);
        }
        SecondFactorMethod secondFactorMethod = (completeInviteRequest != null) ? completeInviteRequest.getSecondFactor() : null;
        return inviteService.complete(inviteCode, secondFactorMethod);
    }

    @GetMapping("/v1/api/invites")
    @JsonMediaTypes
    @Operation(
            summary = "List invites for a service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Invite.class))))
            }
    )
    public ResponseEntity<?> getInvites(@Parameter(example = "ahq8745yq387") @RequestParam(value = "serviceId", required = false) String serviceId) {
        LOGGER.info("List invites GET request for service - [ {} ]", serviceId);
        List<Invite> invites = inviteFinder.findAllActiveInvites(serviceId);
        return ResponseEntity.status(OK).contentType(APPLICATION_JSON).body(invites);
    }

    @PostMapping("/v1/api/invites/create-self-registration-invite")
    @JsonMediaTypes(consumes = true)
    @Operation(
            summary = "Creates an invitation to allow self provisioning new service with Pay",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "422", description = "Missing required fields or invalid values"),
                    @ApiResponse(responseCode = "403", description = "The email is not an allowed public sector email address")
            }
    )
    public ResponseEntity<?> createSelfRegistrationInvite(@org.springframework.web.bind.annotation.RequestBody(required = false) @Valid CreateSelfRegistrationInviteRequest createSelfRegistrationInviteRequest) {
        LOGGER.info("Initiating create self-registration invitation request");
        if (!isPublicSectorEmail(createSelfRegistrationInviteRequest.getEmail())) {
            throw AdminUsersExceptions.invalidPublicSectorEmail(createSelfRegistrationInviteRequest.getEmail());
        }
        Invite invite = selfRegistrationInviteCreator.doInvite(createSelfRegistrationInviteRequest);
        return ResponseEntity.status(CREATED).body(invite);
    }

    @PostMapping("/v1/api/invites/create-invite-to-join-service")
    @JsonMediaTypes(consumes = true)
    @Operation(
            summary = "Creates an invitation to allow a new team member to join an existing service.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "409", description = "Invite has already been sent to the corresponding email address"),
                    @ApiResponse(responseCode = "412", description = "User is already registered on the service"),
                    @ApiResponse(responseCode = "422", description = "Missing required fields or invalid values"),
                    @ApiResponse(responseCode = "404", description = "Service or role not found")
            }
    )
    public ResponseEntity<?> createInviteToJoinService(@org.springframework.web.bind.annotation.RequestBody(required = false) @Valid CreateInviteToJoinServiceRequest createInviteToJoinServiceRequest) {
        LOGGER.info("Initiating create invite to join service request");
        return joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest)
                .map(invite -> ResponseEntity.status(CREATED).body(invite))
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
    }

    @PostMapping("/v2/api/invites/otp/validate")
    @JsonMediaTypes(consumes = true)
    @Operation(
            summary = "Validates OTP for the invite",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No content"),
                    @ApiResponse(responseCode = "422", description = "Missing required fields or invalid values"),
                    @ApiResponse(responseCode = "404", description = "Invite not found")
            }
    )
    public ResponseEntity<Void> validateOtpKey(@org.springframework.web.bind.annotation.RequestBody(required = false) @Valid InviteValidateOtpRequest inviteValidateOtpRequest) {
        LOGGER.info("Invite POST request for validating otp");
        inviteService.validateOtp(inviteValidateOtpRequest);
        return ResponseEntity.noContent().build();
    }
}
