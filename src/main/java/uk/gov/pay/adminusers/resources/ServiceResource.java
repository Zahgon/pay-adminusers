package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.model.CreateServiceRequest;
import uk.gov.pay.adminusers.model.GovUkPayAgreement;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.SearchServicesResponse;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceSearchRequest;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.StripeAgreement;
import uk.gov.pay.adminusers.model.StripeAgreementRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.service.GovUkPayAgreementService;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.SendLiveAccountCreatedEmailService;
import uk.gov.pay.adminusers.service.ServiceCreator;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.ServiceUpdater;
import uk.gov.pay.adminusers.service.ServiceUserRemover;
import uk.gov.pay.adminusers.service.StripeAgreementService;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import uk.gov.pay.adminusers.exception.WebApplicationException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.pay.adminusers.web.JsonMediaTypes;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
public class ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceResource.class);
    /* default */static final String HEADER_USER_CONTEXT = "GovUkPay-User-Context";

    public static final String FIELD_NAME = "name";

    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;
    private final ServiceRequestValidator serviceRequestValidator;
    private final ServiceCreator serviceCreator;
    private final ServiceUpdater serviceUpdater;
    private final ServiceUserRemover serviceUserRemover;
    private final ServiceFinder serviceFinder;
    private final StripeAgreementService stripeAgreementService;
    private final GovUkPayAgreementRequestValidator govUkPayAgreementRequestValidator;
    private final GovUkPayAgreementService govUkPayAgreementService;
    private final SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService;
    public ServiceResource(UserDao userDao,
                           ServiceDao serviceDao,
                           LinksBuilder linksBuilder,
                           ServiceRequestValidator serviceRequestValidator,
                           ServiceCreator serviceCreator,
                           ServiceUpdater serviceUpdater,
                           ServiceUserRemover serviceUserRemover,
                           ServiceFinder serviceFinder,
                           StripeAgreementService stripeAgreementService,
                           GovUkPayAgreementRequestValidator govUkPayAgreementRequestValidator,
                           GovUkPayAgreementService govUkPayAgreementService,
                           SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
        this.serviceRequestValidator = serviceRequestValidator;
        this.serviceCreator = serviceCreator;
        this.serviceUpdater = serviceUpdater;
        this.serviceUserRemover = serviceUserRemover;
        this.serviceFinder = serviceFinder;
        this.stripeAgreementService = stripeAgreementService;
        this.govUkPayAgreementRequestValidator = govUkPayAgreementRequestValidator;
        this.govUkPayAgreementService = govUkPayAgreementService;
        this.sendLiveAccountCreatedEmailService = sendLiveAccountCreatedEmailService;
    }

    @GetMapping("/v1/api/services/list")
    @JsonMediaTypes
    @Operation(
            summary = "Get all services",
            tags = "Services",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Service.class))))
            }
    )
    public ResponseEntity<?> getServices() {
        LOGGER.info("Get Services request");
        List<Service> services = serviceDao.listAll().stream().map(ServiceEntity::toService).map(linksBuilder::decorate).collect(toUnmodifiableList());
        return ResponseEntity
                .status(OK)
                .body(services);
    }

    @GetMapping("/v1/api/services/{serviceExternalId}")
    @JsonMediaTypes
    @Operation(
            tags = "Services",
            summary = "Find service by external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> findService(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3") @PathVariable("serviceExternalId")
                                String serviceExternalId) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity ->
                        ResponseEntity.status(OK).body(linksBuilder.decorate(serviceEntity.toService())))
                .orElseThrow(() ->
                        new ResponseStatusException(NOT_FOUND));
    }

    @GetMapping("/v1/api/services")
    @JsonMediaTypes
    @Operation(
            tags = "Services",
            summary = "Find service associated with gateway account ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "400", description = "Missing gateway account ID"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
            }
    )
    public ResponseEntity<?> findServices(@Parameter(example = "1") @RequestParam(value = "gatewayAccountId", required = false) String gatewayAccountId) {
        return serviceRequestValidator.validateFindRequest(gatewayAccountId)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> serviceFinder.byGatewayAccountId(gatewayAccountId)
                        .<ResponseEntity<?>>map(service -> ResponseEntity.status(OK).body(service))
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
    }

    @PostMapping("/v1/api/services/search")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Search services by name or merchant name",
            description = "This endpoint returns a list of services using lexical meaning to determine a match to the search criteria",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{" +
                            "    \"service_name\": \"service name\"," +
                            "    \"service_merchant_name\": \"service merchant name\"" +
                            "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = SearchServicesResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON payload")
            }
    )
    public ResponseEntity<?> searchServices(@org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        LOGGER.info("Search services request = [ {} ]", payload);
        var searchRequest = ServiceSearchRequest.from(payload);
        return serviceRequestValidator.validateSearchRequest(searchRequest)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> ResponseEntity
                        .status(OK)
                        .body(serviceFinder.bySearchRequest(searchRequest))
                );
    }

    @PostMapping("/v1/api/services")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Create new service",
            description = "This endpoint creates a new service. And assigns to gateway account ids (Optional). <br> `service_name` keys are supported ISO-639-1 language codes and values are translated service names | key must be `\"en\"` or `\"cy\"`",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{" +
                            "    \"gateway_account_ids\": [\"1\"]," +
                            "    \"service_name\": {" +
                            "      \"en\": \"Some service name\"," +
                            "      \"cy\": \"Service name in welsh\"" +
                            "    }" +
                            "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "409", description = "Gateway account IDs provided has already been assigned to another service"),
                    @ApiResponse(responseCode = "400", description = "Service name keys must be one of 'en' (English) or 'cy' (Welsh)"),
                    @ApiResponse(responseCode = "500", description = "Invalid JSON payload")
            }
    )
    public ResponseEntity<?> createService(@org.springframework.web.bind.annotation.RequestBody(required = false) @Valid CreateServiceRequest createServiceRequest) {
        LOGGER.info("Create Service POST request - [ {} ]", createServiceRequest);

        List<String> gatewayAccountIds = Optional.ofNullable(createServiceRequest).map(CreateServiceRequest::gatewayAccountIds).orElse(List.of());
        Map<SupportedLanguage, String> serviceNames = Optional.ofNullable(createServiceRequest).map(CreateServiceRequest::serviceName).orElse(Map.of());
        Service service = serviceCreator.doCreate(gatewayAccountIds, serviceNames);
        return ResponseEntity.status(CREATED).body(service);

    }

    @PatchMapping("/v1/api/services/{serviceExternalId}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Patch service attributes",
            description = "Allows patching below service attributes. Each attribute has its own validation depending on data type." +
                    " Request can either be a single object or an array of objects. It’s similar to (but not 100% compliant with) [JSON Patch](http://jsonpatch.com/)." +
                    "<br>\n " +
                    " " +
                    "| op |  field | example |\n" +
                    "| --- |  --- | ----|\n" +
                    "| add | gateway_account_ids  | [\"1\"] |\n" +
                    "| replace | redirect_to_service_immediately_on_terminal_state | false |\n" +
                    "| replace | experimental_features_enabled | false |\n" +
                    "| replace | takes_payments_over_phone | false |\n" +
                    "| replace | agent_initiated_moto_enabled | false |\n" +
                    "| replace | collect_billing_address | true |\n" +
                    "| replace | current_go_live_stage | NOT_STARTED |\n" +
                    "| replace | current_psp_test_account_stage | NOT_STARTED |\n" +
                    "| replace | merchant_details/name, organisatio | name |\n" +
                    "| replace | merchant_details/address_line1, Address lin | 1 |\n" +
                    "| replace | merchant_details/address_line2, Address lin | 2  |\n" +
                    "| replace | merchant_details/address_city | London |\n" +
                    "| replace | merchant_details/address_country | GB |\n" +
                    "| replace | merchant_details/address_postcode | E6 8XX |\n" +
                    "| replace | merchant_detail |/email,  |\n" +
                    "| replace | merchant_details/email, email@exampl |.com |\n" +
                    "| replace | merchant_detail |/url,  |\n" +
                    "| replace | merchant_details/url, http://www.exampl |.org |\n" +
                    "| replace | merchant_detail |/telephone_number,  |\n" +
                    "| replace | merchant_details/telephone_number | 447700900000 |\n" +
                    "| replace | custom_branding | { \"css_url\": \"css url\", \"image_url\": \"image url\"} |\n" +
                    "| replace | custom_branding | {} |\n" +
                    "| replace | service_name/en | Some service name |\n" +
                    "| replace | sector | local government |\n" +
                    "| replace | internal | true |\n" +
                    "| replace | archived | true |\n" +
                    "| replace | went_live_date | 2022-04-09T18:07:46Z |\n" +
                    "| replace | default_billing_address_country | GB | ",
            requestBody = @RequestBody(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceUpdateRequest.class)))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public ResponseEntity<?> updateServiceAttribute(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                           @PathVariable("serviceExternalId") String serviceExternalId,
                                           @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        return serviceRequestValidator.validateUpdateAttributeRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> processUpdateServiceAttributePayload(serviceExternalId, payload));
    }

    private ResponseEntity<?> processUpdateServiceAttributePayload(String serviceExternalId, JsonNode payload) {

        final List<ServiceUpdateRequest> requests = ServiceUpdateRequest.getUpdateRequests(payload);
        return serviceUpdater.doUpdate(serviceExternalId, requests)
                .map(service -> ResponseEntity.status(OK).body(service))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @PutMapping("/v1/api/services/{serviceExternalId}/merchant-details")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Update merchant details of a service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
            }
    )
    public ResponseEntity<?> updateServiceMerchantDetails(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                                 @PathVariable("serviceExternalId") String serviceExternalId,
                                                 @Parameter(schema = @Schema(implementation = UpdateMerchantDetailsRequest.class))
                                                 @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload)
            throws ValidationException, ServiceNotFoundException {
        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
        Service service = serviceUpdater.doUpdateMerchantDetails(
                serviceExternalId, UpdateMerchantDetailsRequest.from(payload));
        return ResponseEntity.status(OK).body(service);
    }

    @GetMapping("/v1/api/services/{serviceExternalId}/users")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Find users of a service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class)))),
                    @ApiResponse(responseCode = "400", description = "If query parameter 'role' is not one of [admin, " +
                            "view-and-refund, view-only, view-and-initiate-moto, view-refund-and-initiate-moto, super-admin]"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public ResponseEntity<?> findUsersByServiceId(@PathVariable("serviceExternalId") String serviceExternalId, 
                                         @RequestParam(value = "role", required = false) RoleName role) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity ->
                        ResponseEntity.status(200).body(
                                userDao.findByServiceId(serviceEntity.getId(), role)
                                        .stream()
                                        .map(UserEntity::toUser)
                                        .map(linksBuilder::decorate)
                                        .toList()
                        )
                ).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    // To consider for all the operations add @HeaderParam("GovUkPay-User-Context") and creating a filter
    // so we could map permissions with Regex URLs and Http method passed on to this filter.
    @DeleteMapping("/v1/api/services/{serviceExternalId}/users/{userExternalId}")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Delete user from a service",
            responses = {
                    @ApiResponse(responseCode = "204", description = "OK"),
                    @ApiResponse(responseCode = "403", description = "Forbidden. `GovUkPay-User-Context` header is blank"),
                    @ApiResponse(responseCode = "409", description = "Conflict. `GovUkPay-User-Context` is same as userExternalId or user with `userExternalId` is not admin of the service"),
            }
    )
    public ResponseEntity<?> removeUserFromService(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                          @PathVariable("serviceExternalId") String serviceExternalId,
                                          @Parameter(example = "0ddf69c1ba924deca07f0ee748ff1533", description = "Admin user external ID of the service")
                                          @PathVariable("userExternalId") String userExternalId,
                                          @Parameter(example = "d012mkldfdfnsdhqha7f0ee748ff1546", required = true, description = "User external ID to remove from service")
                                          @RequestHeader(value = HEADER_USER_CONTEXT, required = false) String userContext) {
        if (isBlank(userContext)) {
            throw new ResponseStatusException(FORBIDDEN);
        } else if (userExternalId.equals(userContext)) {
            throw new ResponseStatusException(CONFLICT);
        }
        serviceUserRemover.remove(userExternalId, userContext, serviceExternalId);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @PostMapping("/v1/api/services/{serviceExternalId}/stripe-agreement")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Record acceptance of Stripe terms",
            description = "Records that a GOV.UK Pay agreement has been accepted for the service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stripe agreement already created"),
                    @ApiResponse(responseCode = "201", description = "Created"),
                    @ApiResponse(responseCode = "422", description = "Invalid JSON payload or IP address")
            }
    )
    public ResponseEntity<?> createStripeAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                          @PathVariable("serviceExternalId") String serviceExternalId,
                                          @NotNull @Valid @org.springframework.web.bind.annotation.RequestBody(required = false) StripeAgreementRequest stripeAgreementRequest) throws UnknownHostException {
        LOGGER.info("Create stripe agreement POST request - [ {} ]", stripeAgreementRequest.toString());

        Optional<StripeAgreement> maybeStripeAgreement = stripeAgreementService.findStripeAgreementByServiceId(serviceExternalId);
        if (maybeStripeAgreement.isPresent()) {
            LOGGER.info("Stripe agreement information is already stored for this service");
            return ResponseEntity.status(OK).build();
        }
        
        stripeAgreementService.doCreate(serviceExternalId,
                InetAddress.getByName(stripeAgreementRequest.getIpAddress()));

        return ResponseEntity.status(CREATED).build();
    }

    @GetMapping("/v1/api/services/{serviceExternalId}/stripe-agreement")
    @JsonMediaTypes
    @Operation(
            tags = "Services",
            summary = "Get details about the acceptance of Stripe terms",
            description = "Retrieves the IP address and timestamp that the Stripe terms were accepted on for the service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = StripeAgreement.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public StripeAgreement getStripeAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                              @PathVariable("serviceExternalId") String serviceExternalId) {

        return stripeAgreementService.findStripeAgreementByServiceId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
    }

    @PostMapping("/v1/api/services/{serviceExternalId}/govuk-pay-agreement")
    @JsonMediaTypes(consumes = true)
    @Operation(
            tags = "Services",
            summary = "Record acceptance of GOV.UK Pay terms",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(requiredProperties = "user_external_id",
                            example = "{" +
                                    "    \"user_external_id\": \"12e3eccfab284ae5bc1108e9c0456ba7\"" +
                                    "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = GovUkPayAgreement.class))),
                    @ApiResponse(responseCode = "200", description = "Agreement already created - existing agreement returned",
                            content = @Content(schema = @Schema(implementation = GovUkPayAgreement.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Service with serviceExternalId not found")
            }
    )
    public ResponseEntity<?> createGovUkPayAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                            @PathVariable("serviceExternalId") String serviceExternalId, @org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) {
        return govUkPayAgreementRequestValidator.validateCreateRequest(payload)
                .<ResponseEntity<?>>map(errors -> ResponseEntity.status(BAD_REQUEST).body(errors))
                .orElseGet(() -> createGovUkPayAgreementFromPayload(serviceExternalId, payload));
    }

    @GetMapping("/v1/api/services/{serviceExternalId}/govuk-pay-agreement")
    @JsonMediaTypes
    @Operation(
            tags = "Services",
            summary = "Get details about the acceptance of GOV.UK Pay terms",
            description = "Retrieves the user's email address and timestamp that the GOV.UK Pay terms were accepted on for the service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = GovUkPayAgreement.class))),
                    @ApiResponse(responseCode = "404", description = "Service with serviceExternalId not found"),
            }
    )
    public ResponseEntity<?> getGovUkPayAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                         @PathVariable("serviceExternalId") String serviceExternalId) {
        return govUkPayAgreementService.findGovUkPayAgreementByServiceId(serviceExternalId)
                .map(agreement -> ResponseEntity.status(OK).body(agreement))
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
    }

    private ResponseEntity<?> createGovUkPayAgreementFromPayload(String serviceExternalId, JsonNode payload) {
        return govUkPayAgreementService.findGovUkPayAgreementByServiceId(serviceExternalId).<ResponseEntity<?>>map(govUkPayAgreement -> {
            LOGGER.info("GOV.UK Pay agreement information is already stored for this service");
            return ResponseEntity.status(OK).body(govUkPayAgreement);
        }).orElseGet(() -> {
            ServiceEntity serviceEntity = serviceDao.findByExternalId(serviceExternalId)
                    .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
            Optional<UserEntity> userEntity = userDao.findByExternalId(payload.get("user_external_id").asText());
            if (!userEntity.isPresent()) {
                return ResponseEntity.status(BAD_REQUEST).body(Errors.from("Field [user_external_id] must be a valid user ID"));
            }
            if (!userEntity.get().getServicesRole(serviceExternalId).isPresent()) {
                return ResponseEntity.status(BAD_REQUEST).body(Errors.from("User does not belong to the given service"));
            }
            GovUkPayAgreement govUkPayAgreement = govUkPayAgreementService.doCreate(serviceEntity, userEntity.get().getEmail(), ZonedDateTime.now(ZoneOffset.UTC));

            return ResponseEntity.status(CREATED).body(govUkPayAgreement);
        });
    }

    @PostMapping("/v1/api/services/{serviceExternalId}/send-live-email")
    @JsonMediaTypes
    @Operation(
            tags = "Services",
            summary = "Sends an email to the user who signed the service agreement to inform them that their service is live",
            description = "This endpoint will send an email to the user who signed the agreement with GOV.UK Pay for the service informing them that their service is now live." +
                    "The email address used is the email address of the user provided to the [POST /v1/api/services/`{serviceExternalId}`/govuk-pay-agreement] endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Service with serviceExternalId not found"),
            }
    )
    public ResponseEntity<?> sendLiveAccountCreatedEmail(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                                @PathVariable("serviceExternalId") String serviceExternalId) {
        serviceDao.findByExternalId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
        sendLiveAccountCreatedEmailService.sendEmail(serviceExternalId);
        return ResponseEntity.status(OK).build();
    }
}
