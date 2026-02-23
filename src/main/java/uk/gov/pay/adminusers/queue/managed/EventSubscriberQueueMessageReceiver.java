package uk.gov.pay.adminusers.queue.managed;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.EventSubscriberQueueConfig;
import uk.gov.pay.adminusers.queue.event.EventMessageHandler;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class EventSubscriberQueueMessageReceiver implements SmartLifecycle {

    private static final String THREAD_NAME = "sqs-message-eventSubscriberQueueMessageReceiver";
    private static final int SCHEDULER_NUMBER_OF_THREADS = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EventMessageHandler eventMessageHandler;
    private final ScheduledExecutorService scheduledExecutorService;

    private final int queueSchedulerThreadDelayInMilliseconds;
    private final int queueSchedulerShutdownTimeoutInSeconds;
    private boolean queueEnabled;
    private volatile boolean running;

    public EventSubscriberQueueMessageReceiver(EventMessageHandler eventMessageHandler,
                                               AdminUsersConfig adminUsersConfig) {
        this.eventMessageHandler = eventMessageHandler;

        scheduledExecutorService = Executors.newScheduledThreadPool(SCHEDULER_NUMBER_OF_THREADS,
                new ThreadFactoryBuilder().setNameFormat(THREAD_NAME + "-%d").build());

        EventSubscriberQueueConfig eventSubscriberQueueConfig = adminUsersConfig.getEventSubscriberQueueConfig();
        queueEnabled = eventSubscriberQueueConfig.getEventSubscriberQueueEnabled();
        queueSchedulerThreadDelayInMilliseconds = eventSubscriberQueueConfig.getQueueSchedulerThreadDelayInMilliseconds();
        queueSchedulerShutdownTimeoutInSeconds = eventSubscriberQueueConfig.getQueueSchedulerShutdownTimeoutInSeconds();
    }

    @Override
    public void start() {
        running = true;
        if (queueEnabled) {
            int initialDelay = queueSchedulerThreadDelayInMilliseconds;
            scheduledExecutorService.scheduleWithFixedDelay(
                    this::processMessages,
                    initialDelay,
                    queueSchedulerThreadDelayInMilliseconds,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void processMessages() {
        logger.info("Queue message receiver thread polling queue");
        try {
            eventMessageHandler.processMessages();
        } catch (Exception e) {
            logger.error("Queue message receiver thread exception", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop() {
        running = false;
        logger.info("Shutting down event subscriber queue message receiver");
        scheduledExecutorService.shutdown();
        try {
            if (scheduledExecutorService.awaitTermination(queueSchedulerShutdownTimeoutInSeconds, TimeUnit.SECONDS)) {
                logger.info("Event subscriber queue message receiver shut down cleanly");
            } else {
                logger.error("Event subscriber queue still processing messages after shutdown wait time will now be forcefully stopped");
                scheduledExecutorService.shutdownNow();
                if (!scheduledExecutorService.awaitTermination(12, TimeUnit.SECONDS)) {
                    logger.error("Event subscriber queue receiver could not be forced stopped");
                }
            }
        } catch (InterruptedException ex) {
            logger.error("Failed to shutdown event subscriber queue message receiver cleanly as the wait was interrupted.");
            scheduledExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
