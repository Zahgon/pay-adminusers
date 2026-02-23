package uk.gov.pay.adminusers.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class NotifyDirectDebitConfiguration {

    @Valid
    @NotNull
    private String mandateCancelledEmailTemplateId;

    @Valid
    @NotNull
    private String mandateFailedEmailTemplateId;

    @Valid
    @NotNull
    private String paymentFailedEmailTemplateId;

    @Valid
    @NotNull
    private String oneOffMandateAndPaymentCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String onDemandMandateCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String onDemandPaymentConfirmedEmailTemplateId;

    public String getMandateCancelledEmailTemplateId() {
        return mandateCancelledEmailTemplateId;
    }

    public String getMandateFailedEmailTemplateId() {
        return mandateFailedEmailTemplateId;
    }

    public String getPaymentFailedEmailTemplateId() {
        return paymentFailedEmailTemplateId;
    }

    public String getOneOffMandateAndPaymentCreatedEmailTemplateId() {
        return oneOffMandateAndPaymentCreatedEmailTemplateId;
    }

    public String getOnDemandMandateCreatedEmailTemplateId() {
        return onDemandMandateCreatedEmailTemplateId;
    }

    public String getOnDemandPaymentConfirmedEmailTemplateId() {
        return onDemandPaymentConfirmedEmailTemplateId;
    }

    public void setMandateCancelledEmailTemplateId(String mandateCancelledEmailTemplateId) {
        this.mandateCancelledEmailTemplateId = mandateCancelledEmailTemplateId;
    }

    public void setMandateFailedEmailTemplateId(String mandateFailedEmailTemplateId) {
        this.mandateFailedEmailTemplateId = mandateFailedEmailTemplateId;
    }

    public void setPaymentFailedEmailTemplateId(String paymentFailedEmailTemplateId) {
        this.paymentFailedEmailTemplateId = paymentFailedEmailTemplateId;
    }

    public void setOneOffMandateAndPaymentCreatedEmailTemplateId(String oneOffMandateAndPaymentCreatedEmailTemplateId) {
        this.oneOffMandateAndPaymentCreatedEmailTemplateId = oneOffMandateAndPaymentCreatedEmailTemplateId;
    }

    public void setOnDemandMandateCreatedEmailTemplateId(String onDemandMandateCreatedEmailTemplateId) {
        this.onDemandMandateCreatedEmailTemplateId = onDemandMandateCreatedEmailTemplateId;
    }

    public void setOnDemandPaymentConfirmedEmailTemplateId(String onDemandPaymentConfirmedEmailTemplateId) {
        this.onDemandPaymentConfirmedEmailTemplateId = onDemandPaymentConfirmedEmailTemplateId;
    }
}
