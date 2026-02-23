package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.EmailService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import uk.gov.pay.adminusers.web.JsonMediaTypes;


import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

//TODO remove this resource as it's only applicable for sending direct debit emails
@RestController
public class EmailResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailResource.class);

    private final EmailService notificationService;
    private final EmailRequestParser emailRequestParser;
    public EmailResource(EmailService notificationService, EmailRequestParser emailRequestParser) {
        this.notificationService = notificationService;
        this.emailRequestParser = emailRequestParser;
    }

    @PostMapping("/v1/emails/send")
    @JsonMediaTypes(consumes = true)
    public ResponseEntity<?> sendEmail(@org.springframework.web.bind.annotation.RequestBody(required = false) JsonNode payload) throws InvalidEmailRequestException, InvalidMerchantDetailsException {
        LOGGER.info("Received email request");
        EmailRequest emailRequest = emailRequestParser.parse(payload);
        EmailTemplate template = emailRequest.getTemplate();
        String gatewayAccountId = emailRequest.getGatewayAccountId();
        LOGGER.info("Sending {} email for account {}", template, gatewayAccountId);
        notificationService.sendEmail(
                emailRequest.getEmailAddress(),
                gatewayAccountId,
                template,
                emailRequest.getPersonalisation());
        return ResponseEntity.status(OK).build();
    }
}
