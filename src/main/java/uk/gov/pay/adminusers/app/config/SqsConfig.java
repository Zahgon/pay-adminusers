package uk.gov.pay.adminusers.app.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public class SqsConfig {

    @NotNull
    private boolean nonStandardServiceEndpoint;
    private String endpoint;
    @NotNull
    private String region;
    private String accessKey;
    private String secretKey;
    
    @NotNull
    private String eventSubscriberQueueUrl;
    
    @NotNull
    private String connectorTasksQueueUrl;

    @Max(20)
    private int messageMaximumWaitTimeInSeconds;
    @Max(10)
    private int messageMaximumBatchSize;

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isNonStandardServiceEndpoint() {
        return nonStandardServiceEndpoint;
    }
    
    public String getEventSubscriberQueueUrl() {
        return eventSubscriberQueueUrl;
    }

    public int getMessageMaximumWaitTimeInSeconds() {
        return messageMaximumWaitTimeInSeconds;
    }

    public int getMessageMaximumBatchSize() {
        return messageMaximumBatchSize;
    }

    public String getConnectorTasksQueueUrl() {
        return connectorTasksQueueUrl;
    }

    public void setNonStandardServiceEndpoint(boolean nonStandardServiceEndpoint) {
        this.nonStandardServiceEndpoint = nonStandardServiceEndpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setEventSubscriberQueueUrl(String eventSubscriberQueueUrl) {
        this.eventSubscriberQueueUrl = eventSubscriberQueueUrl;
    }

    public void setConnectorTasksQueueUrl(String connectorTasksQueueUrl) {
        this.connectorTasksQueueUrl = connectorTasksQueueUrl;
    }

    public void setMessageMaximumWaitTimeInSeconds(int messageMaximumWaitTimeInSeconds) {
        this.messageMaximumWaitTimeInSeconds = messageMaximumWaitTimeInSeconds;
    }

    public void setMessageMaximumBatchSize(int messageMaximumBatchSize) {
        this.messageMaximumBatchSize = messageMaximumBatchSize;
    }
}
