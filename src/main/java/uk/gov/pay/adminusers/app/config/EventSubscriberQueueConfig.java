package uk.gov.pay.adminusers.app.config;

public class EventSubscriberQueueConfig {

    private Boolean eventSubscriberQueueEnabled;
    private int queueSchedulerNumberOfThreads;
    private int queueSchedulerThreadDelayInMilliseconds;
    private int failedMessageRetryDelayInSeconds;
    private int queueSchedulerShutdownTimeoutInSeconds;

    public Boolean getEventSubscriberQueueEnabled() {
        return eventSubscriberQueueEnabled;
    }

    public int getQueueSchedulerNumberOfThreads() {
        return queueSchedulerNumberOfThreads;
    }

    public int getQueueSchedulerThreadDelayInMilliseconds() {
        return queueSchedulerThreadDelayInMilliseconds;
    }

    public int getFailedMessageRetryDelayInSeconds() {
        return failedMessageRetryDelayInSeconds;
    }

    public int getQueueSchedulerShutdownTimeoutInSeconds() {
        return queueSchedulerShutdownTimeoutInSeconds;
    }

    public void setEventSubscriberQueueEnabled(Boolean eventSubscriberQueueEnabled) {
        this.eventSubscriberQueueEnabled = eventSubscriberQueueEnabled;
    }

    public void setQueueSchedulerNumberOfThreads(int queueSchedulerNumberOfThreads) {
        this.queueSchedulerNumberOfThreads = queueSchedulerNumberOfThreads;
    }

    public void setQueueSchedulerThreadDelayInMilliseconds(int queueSchedulerThreadDelayInMilliseconds) {
        this.queueSchedulerThreadDelayInMilliseconds = queueSchedulerThreadDelayInMilliseconds;
    }

    public void setFailedMessageRetryDelayInSeconds(int failedMessageRetryDelayInSeconds) {
        this.failedMessageRetryDelayInSeconds = failedMessageRetryDelayInSeconds;
    }

    public void setQueueSchedulerShutdownTimeoutInSeconds(int queueSchedulerShutdownTimeoutInSeconds) {
        this.queueSchedulerShutdownTimeoutInSeconds = queueSchedulerShutdownTimeoutInSeconds;
    }
}
