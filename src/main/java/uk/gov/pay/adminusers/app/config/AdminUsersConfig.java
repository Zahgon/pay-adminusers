package uk.gov.pay.adminusers.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Optional;

/**
 * Settings of the adminusers service, bound from the {@code adminusers} section of {@code application.yaml}, which
 * takes them from the environment variables the service has always used, with the same defaults.
 */
@Validated
@ConfigurationProperties(prefix = "adminusers")
public class AdminUsersConfig {

    @NotNull
    private String environment;

    @Valid
    @NotNull
    private JPAConfiguration jpaConfiguration;

    @NotNull
    private String baseUrl;

    @Valid
    @NotNull
    private final LinksConfig links = new LinksConfig();

    @NotNull
    private Integer loginAttemptCap;

    @NotNull
    private NotifyConfiguration notifyConfiguration;

    @NotNull
    private NotifyDirectDebitConfiguration notifyDirectDebitConfiguration;

    @NotNull
    private Integer forgottenPasswordExpiryMinutes;

    @NotNull
    private SecondFactorAuthConfiguration secondFactorAuthConfiguration;

    @Valid
    @NotNull
    private SqsConfig sqsConfig;

    @NotNull
    private EventSubscriberQueueConfig eventSubscriberQueueConfig;

    @NotNull
    private ConnectorTaskQueueConfig connectorTaskQueueConfig;

    @NotNull
    private ExpungeAndArchiveDataConfig expungeAndArchiveDataConfig;

    @NotNull
    private String ledgerBaseUrl;

    @Valid
    @NotNull
    private RestClientConfig restClientConfig;

    private URI ecsContainerMetadataUriV4;

    public SecondFactorAuthConfiguration getSecondFactorAuthConfiguration() {
        return secondFactorAuthConfiguration;
    }

    public JPAConfiguration getJpaConfiguration() {
        return jpaConfiguration;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Integer getLoginAttemptCap() {
        return loginAttemptCap;
    }

    public LinksConfig getLinks() {
        return links;
    }
    public NotifyConfiguration getNotifyConfiguration() {
        return notifyConfiguration;
    }
    public NotifyDirectDebitConfiguration getNotifyDirectDebitConfiguration() {
        return notifyDirectDebitConfiguration;
    }

    public Integer getForgottenPasswordExpiryMinutes() {
        return forgottenPasswordExpiryMinutes;
    }
    public SqsConfig getSqsConfig() {
        return sqsConfig;
    }
    public EventSubscriberQueueConfig getEventSubscriberQueueConfig() {
        return eventSubscriberQueueConfig;
    }

    public String getLedgerBaseUrl() {
        return ledgerBaseUrl;
    }

    public RestClientConfig getRestClientConfig() {
        return restClientConfig;
    }

    public ExpungeAndArchiveDataConfig getExpungeAndArchiveDataConfig() {
        return expungeAndArchiveDataConfig;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }

    public ConnectorTaskQueueConfig getConnectorTaskQueueConfig() {
        return connectorTaskQueueConfig;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setJpaConfiguration(JPAConfiguration jpaConfiguration) {
        this.jpaConfiguration = jpaConfiguration;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setLoginAttemptCap(Integer loginAttemptCap) {
        this.loginAttemptCap = loginAttemptCap;
    }

    public void setNotifyConfiguration(NotifyConfiguration notifyConfiguration) {
        this.notifyConfiguration = notifyConfiguration;
    }

    public void setNotifyDirectDebitConfiguration(NotifyDirectDebitConfiguration notifyDirectDebitConfiguration) {
        this.notifyDirectDebitConfiguration = notifyDirectDebitConfiguration;
    }

    public void setForgottenPasswordExpiryMinutes(Integer forgottenPasswordExpiryMinutes) {
        this.forgottenPasswordExpiryMinutes = forgottenPasswordExpiryMinutes;
    }

    public void setSecondFactorAuthConfiguration(SecondFactorAuthConfiguration secondFactorAuthConfiguration) {
        this.secondFactorAuthConfiguration = secondFactorAuthConfiguration;
    }

    public void setSqsConfig(SqsConfig sqsConfig) {
        this.sqsConfig = sqsConfig;
    }

    public void setEventSubscriberQueueConfig(EventSubscriberQueueConfig eventSubscriberQueueConfig) {
        this.eventSubscriberQueueConfig = eventSubscriberQueueConfig;
    }

    public void setConnectorTaskQueueConfig(ConnectorTaskQueueConfig connectorTaskQueueConfig) {
        this.connectorTaskQueueConfig = connectorTaskQueueConfig;
    }

    public void setExpungeAndArchiveDataConfig(ExpungeAndArchiveDataConfig expungeAndArchiveDataConfig) {
        this.expungeAndArchiveDataConfig = expungeAndArchiveDataConfig;
    }

    public void setLedgerBaseUrl(String ledgerBaseUrl) {
        this.ledgerBaseUrl = ledgerBaseUrl;
    }

    public void setRestClientConfig(RestClientConfig restClientConfig) {
        this.restClientConfig = restClientConfig;
    }

    public void setEcsContainerMetadataUriV4(URI ecsContainerMetadataUriV4) {
        this.ecsContainerMetadataUriV4 = ecsContainerMetadataUriV4;
    }
}
