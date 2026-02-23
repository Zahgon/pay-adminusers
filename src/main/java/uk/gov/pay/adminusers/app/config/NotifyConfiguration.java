package uk.gov.pay.adminusers.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class NotifyConfiguration {

    @Valid
    @NotNull
    private String cardApiKey;

    @Valid
    @NotNull
    private String notificationBaseURL;

    @Valid
    @NotNull
    private String signInOtpSmsTemplateId;

    @Valid
    @NotNull
    private String changeSignIn2faToSmsOtpSmsTemplateId;

    @Valid
    @NotNull
    private String selfInitiatedCreateUserAndServiceOtpSmsTemplateId;

    @Valid
    @NotNull
    private String createUserInResponseToInvitationToServiceOtpSmsTemplateId;

    @Valid
    @NotNull
    private String inviteUserEmailTemplateId;

    @Valid
    @NotNull
    private String inviteUserExistingEmailTemplateId;

    @Valid
    @NotNull
    private String inviteServiceEmailTemplateId;

    @Valid
    @NotNull
    private String forgottenPasswordEmailTemplateId;

    @Valid
    @NotNull
    private String inviteServiceUserExistsEmailTemplateId;

    @Valid
    @NotNull
    private String inviteServiceUserDisabledEmailTemplateId;

    @Valid
    @NotNull
    private String liveAccountCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeLostEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeLostAndServiceChargedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeEvidenceSubmittedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeWonEmailTemplateId;

    @Valid
    @NotNull
    private String notifyEmailReplyToSupportId;

    public String getCardApiKey() {
        return cardApiKey;
    }

    public String getNotificationBaseURL() {
        return notificationBaseURL;
    }

    public String getSignInOtpSmsTemplateId() {
        return signInOtpSmsTemplateId;
    }

    public String getChangeSignIn2faToSmsOtpSmsTemplateId() {
        return changeSignIn2faToSmsOtpSmsTemplateId;
    }

    public String getSelfInitiatedCreateUserAndServiceOtpSmsTemplateId() {
        return selfInitiatedCreateUserAndServiceOtpSmsTemplateId;
    }

    public String getCreateUserInResponseToInvitationToServiceOtpSmsTemplateId() {
        return createUserInResponseToInvitationToServiceOtpSmsTemplateId;
    }

    public String getInviteUserEmailTemplateId() {
        return inviteUserEmailTemplateId;
    }

    public String getForgottenPasswordEmailTemplateId() {
        return forgottenPasswordEmailTemplateId;
    }

    public String getInviteServiceEmailTemplateId() {
        return inviteServiceEmailTemplateId;
    }

    public String getInviteServiceUserExistsEmailTemplateId() {
        return inviteServiceUserExistsEmailTemplateId;
    }

    public String getInviteServiceUserDisabledEmailTemplateId() {
        return inviteServiceUserDisabledEmailTemplateId;
    }

    public String getInviteUserExistingEmailTemplateId() {
        return inviteUserExistingEmailTemplateId;
    }

    public String getLiveAccountCreatedEmailTemplateId() {
        return liveAccountCreatedEmailTemplateId;
    }

    public String getStripeDisputeCreatedEmailTemplateId() {
        return stripeDisputeCreatedEmailTemplateId;
    }

    public String getStripeDisputeLostEmailTemplateId() {
        return stripeDisputeLostEmailTemplateId;
    }

    public String getStripeDisputeLostAndServiceChargedEmailTemplateId() {
        return stripeDisputeLostAndServiceChargedEmailTemplateId;
    }

    public String getStripeDisputeEvidenceSubmittedEmailTemplateId() {
        return stripeDisputeEvidenceSubmittedEmailTemplateId;
    }

    public String getStripeDisputeWonEmailTemplateId() {
        return stripeDisputeWonEmailTemplateId;
    }

    public String getNotifyEmailReplyToSupportId() {
        return notifyEmailReplyToSupportId;
    }

    public void setCardApiKey(String cardApiKey) {
        this.cardApiKey = cardApiKey;
    }

    public void setNotificationBaseURL(String notificationBaseURL) {
        this.notificationBaseURL = notificationBaseURL;
    }

    public void setSignInOtpSmsTemplateId(String signInOtpSmsTemplateId) {
        this.signInOtpSmsTemplateId = signInOtpSmsTemplateId;
    }

    public void setChangeSignIn2faToSmsOtpSmsTemplateId(String changeSignIn2faToSmsOtpSmsTemplateId) {
        this.changeSignIn2faToSmsOtpSmsTemplateId = changeSignIn2faToSmsOtpSmsTemplateId;
    }

    public void setSelfInitiatedCreateUserAndServiceOtpSmsTemplateId(String selfInitiatedCreateUserAndServiceOtpSmsTemplateId) {
        this.selfInitiatedCreateUserAndServiceOtpSmsTemplateId = selfInitiatedCreateUserAndServiceOtpSmsTemplateId;
    }

    public void setCreateUserInResponseToInvitationToServiceOtpSmsTemplateId(String createUserInResponseToInvitationToServiceOtpSmsTemplateId) {
        this.createUserInResponseToInvitationToServiceOtpSmsTemplateId = createUserInResponseToInvitationToServiceOtpSmsTemplateId;
    }

    public void setInviteUserEmailTemplateId(String inviteUserEmailTemplateId) {
        this.inviteUserEmailTemplateId = inviteUserEmailTemplateId;
    }

    public void setInviteUserExistingEmailTemplateId(String inviteUserExistingEmailTemplateId) {
        this.inviteUserExistingEmailTemplateId = inviteUserExistingEmailTemplateId;
    }

    public void setInviteServiceEmailTemplateId(String inviteServiceEmailTemplateId) {
        this.inviteServiceEmailTemplateId = inviteServiceEmailTemplateId;
    }

    public void setForgottenPasswordEmailTemplateId(String forgottenPasswordEmailTemplateId) {
        this.forgottenPasswordEmailTemplateId = forgottenPasswordEmailTemplateId;
    }

    public void setInviteServiceUserExistsEmailTemplateId(String inviteServiceUserExistsEmailTemplateId) {
        this.inviteServiceUserExistsEmailTemplateId = inviteServiceUserExistsEmailTemplateId;
    }

    public void setInviteServiceUserDisabledEmailTemplateId(String inviteServiceUserDisabledEmailTemplateId) {
        this.inviteServiceUserDisabledEmailTemplateId = inviteServiceUserDisabledEmailTemplateId;
    }

    public void setLiveAccountCreatedEmailTemplateId(String liveAccountCreatedEmailTemplateId) {
        this.liveAccountCreatedEmailTemplateId = liveAccountCreatedEmailTemplateId;
    }

    public void setStripeDisputeCreatedEmailTemplateId(String stripeDisputeCreatedEmailTemplateId) {
        this.stripeDisputeCreatedEmailTemplateId = stripeDisputeCreatedEmailTemplateId;
    }

    public void setStripeDisputeLostEmailTemplateId(String stripeDisputeLostEmailTemplateId) {
        this.stripeDisputeLostEmailTemplateId = stripeDisputeLostEmailTemplateId;
    }

    public void setStripeDisputeLostAndServiceChargedEmailTemplateId(String stripeDisputeLostAndServiceChargedEmailTemplateId) {
        this.stripeDisputeLostAndServiceChargedEmailTemplateId = stripeDisputeLostAndServiceChargedEmailTemplateId;
    }

    public void setStripeDisputeEvidenceSubmittedEmailTemplateId(String stripeDisputeEvidenceSubmittedEmailTemplateId) {
        this.stripeDisputeEvidenceSubmittedEmailTemplateId = stripeDisputeEvidenceSubmittedEmailTemplateId;
    }

    public void setStripeDisputeWonEmailTemplateId(String stripeDisputeWonEmailTemplateId) {
        this.stripeDisputeWonEmailTemplateId = stripeDisputeWonEmailTemplateId;
    }

    public void setNotifyEmailReplyToSupportId(String notifyEmailReplyToSupportId) {
        this.notifyEmailReplyToSupportId = notifyEmailReplyToSupportId;
    }
}
