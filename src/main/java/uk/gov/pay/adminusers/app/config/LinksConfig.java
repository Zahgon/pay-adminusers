package uk.gov.pay.adminusers.app.config;

public class LinksConfig {

    private String selfserviceUrl;
    private String selfserviceInvitesUrl;
    private String selfserviceLoginUrl;
    private String selfserviceForgottenPasswordUrl;
    private String supportUrl;
    private String selfserviceServicesUrl;

    public String getSelfserviceUrl() {
        return selfserviceUrl;
    }

    public String getSelfserviceInvitesUrl() {
        return selfserviceInvitesUrl;
    }

    public String getSelfserviceLoginUrl() {
        return selfserviceLoginUrl;
    }

    public String getSelfserviceForgottenPasswordUrl() {
        return selfserviceForgottenPasswordUrl;
    }

    public String getSupportUrl() {
        return supportUrl;
    }

    public String getSelfserviceServicesUrl() {
        return selfserviceServicesUrl;
    }

    public void setSelfserviceUrl(String selfserviceUrl) {
        this.selfserviceUrl = selfserviceUrl;
    }

    public void setSelfserviceInvitesUrl(String selfserviceInvitesUrl) {
        this.selfserviceInvitesUrl = selfserviceInvitesUrl;
    }

    public void setSelfserviceLoginUrl(String selfserviceLoginUrl) {
        this.selfserviceLoginUrl = selfserviceLoginUrl;
    }

    public void setSelfserviceForgottenPasswordUrl(String selfserviceForgottenPasswordUrl) {
        this.selfserviceForgottenPasswordUrl = selfserviceForgottenPasswordUrl;
    }

    public void setSupportUrl(String supportUrl) {
        this.supportUrl = supportUrl;
    }

    public void setSelfserviceServicesUrl(String selfserviceServicesUrl) {
        this.selfserviceServicesUrl = selfserviceServicesUrl;
    }
}
