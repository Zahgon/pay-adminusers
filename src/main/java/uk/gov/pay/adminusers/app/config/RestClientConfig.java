package uk.gov.pay.adminusers.app.config;

public class RestClientConfig {

    private String disabledSecureConnection;

    public RestClientConfig() {
    }

    public Boolean isDisabledSecureConnection() {
        return "true".equals(disabledSecureConnection);
    }

    public void setDisabledSecureConnection(String disabledSecureConnection) {
        this.disabledSecureConnection = disabledSecureConnection;
    }
}
