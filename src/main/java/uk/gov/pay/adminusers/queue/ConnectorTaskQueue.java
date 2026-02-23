package uk.gov.pay.adminusers.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.queue.model.ConnectorTask;
import uk.gov.pay.adminusers.queue.sqs.QueueException;
import uk.gov.pay.adminusers.queue.sqs.QueueMessage;
import uk.gov.pay.adminusers.queue.sqs.AbstractQueue;
import uk.gov.pay.adminusers.queue.sqs.SqsQueueService;
import org.springframework.stereotype.Component;


import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class ConnectorTaskQueue extends AbstractQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorTaskQueue.class);
    public ConnectorTaskQueue(SqsQueueService sqsQueueService, AdminUsersConfig adminUsersConfig, ObjectMapper objectMapper) {
        super(sqsQueueService, objectMapper, adminUsersConfig.getSqsConfig().getConnectorTasksQueueUrl(),
                adminUsersConfig.getConnectorTaskQueueConfig().getFailedMessageRetryDelayInSeconds());
    }

    public void addTaskToQueue(ConnectorTask task) {
        try {
            String message = objectMapper.writeValueAsString(task);
            QueueMessage queueMessage = sendMessageToQueue(message);
            LOGGER.info("Task added to queue",
                    kv("task_type", task.getTaskType()),
                    kv("message_id", queueMessage.getMessageId()));
        } catch (JsonProcessingException | QueueException e) {
            LOGGER.error("Error adding task to queue",
                    kv("task_name", task.getTaskType()),
                    kv("error", e.getMessage()));
        }
    }
}
