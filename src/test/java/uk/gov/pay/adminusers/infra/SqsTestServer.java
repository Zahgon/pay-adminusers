package uk.gov.pay.adminusers.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.codec.digest.DigestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An in-process stand-in for an SQS broker, speaking the AWS JSON protocol of the SQS API the service uses (create
 * queue, send, receive with long polling, delete and change visibility). Messages that have been received stay
 * invisible for the visibility timeout (30 seconds) unless it is changed, and come back afterwards unless deleted.
 * {@link #close()} stops the server.
 */
public class SqsTestServer implements AutoCloseable {

    private static final Duration DEFAULT_VISIBILITY_TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final ExecutorService executor;
    private final Map<String, List<Message>> queues = new LinkedHashMap<>();

    private SqsTestServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "sqs-test-server");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
    }

    /**
     * Starts a server with the given queues.
     */
    public static SqsTestServer start(List<String> queueNames) {
        SqsTestServer sqsTestServer;
        try {
            sqsTestServer = new SqsTestServer();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the SQS test server", e);
        }
        queueNames.forEach(sqsTestServer::createQueue);
        return sqsTestServer;
    }

    public String getQueueUrl(String queueName) {
        return getEndpoint() + "/queue/" + queueName;
    }

    public String getEndpoint() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /**
     * A client of this server, which the caller closes.
     */
    public SqsClient createSqsClient() {
        // random credentials required by AWS SDK to build SQS client
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create("x", "x");
        return SqsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .endpointOverride(URI.create(getEndpoint()))
                .region(Region.of("region-1"))
                .build();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange; InputStream requestBody = exchange.getRequestBody()) {
            String target = exchange.getRequestHeaders().getFirst("X-Amz-Target");
            JsonNode request = OBJECT_MAPPER.readTree(requestBody.readAllBytes());
            String operation = target == null ? "" : target.substring(target.indexOf('.') + 1);
            ObjectNode response = OBJECT_MAPPER.createObjectNode();
            int status = 200;
            switch (operation) {
                case "CreateQueue" -> response.put("QueueUrl", createQueue(request.path("QueueName").asText()));
                case "GetQueueUrl" -> response.put("QueueUrl", getQueueUrl(request.path("QueueName").asText()));
                case "SendMessage" -> sendMessage(request, response);
                case "ReceiveMessage" -> receiveMessages(request, response);
                case "DeleteMessage" -> changeVisibility(request.path("ReceiptHandle").asText(), null);
                case "ChangeMessageVisibility" -> changeVisibility(request.path("ReceiptHandle").asText(),
                        Duration.ofSeconds(request.path("VisibilityTimeout").asLong()));
                default -> {
                    status = 400;
                    response.put("__type", "com.amazonaws.sqs#UnsupportedOperation");
                    response.put("message", "Unsupported operation " + operation);
                }
            }
            byte[] body = OBJECT_MAPPER.writeValueAsBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/x-amz-json-1.0");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        }
    }

    private synchronized String createQueue(String queueName) {
        queues.computeIfAbsent(queueName, name -> new ArrayList<>());
        return getQueueUrl(queueName);
    }

    private synchronized void sendMessage(JsonNode request, ObjectNode response) {
        String body = request.path("MessageBody").asText();
        Message message = new Message(UUID.randomUUID().toString(), body,
                Instant.now().plusSeconds(request.path("DelaySeconds").asLong(0)));
        queue(request).add(message);
        notifyAll();
        response.put("MD5OfMessageBody", DigestUtils.md5Hex(body.getBytes(UTF_8)));
        response.put("MessageId", message.messageId);
    }

    private synchronized void receiveMessages(JsonNode request, ObjectNode response) {
        List<Message> queue = queue(request);
        int maximumNumberOfMessages = request.path("MaxNumberOfMessages").asInt(1);
        Instant waitUntil = Instant.now().plusSeconds(request.path("WaitTimeSeconds").asLong(0));
        List<Message> received = new ArrayList<>();
        while (true) {
            Instant now = Instant.now();
            for (Message message : queue) {
                if (received.size() < maximumNumberOfMessages && !message.visibleFrom.isAfter(now)) {
                    message.visibleFrom = now.plus(DEFAULT_VISIBILITY_TIMEOUT);
                    message.receiptHandle = UUID.randomUUID().toString();
                    received.add(message);
                }
            }
            long waitMillis = Duration.between(Instant.now(), waitUntil).toMillis();
            if (!received.isEmpty() || waitMillis <= 0) {
                break;
            }
            try {
                wait(Math.min(waitMillis, 100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        ArrayNode messages = response.putArray("Messages");
        received.forEach(message -> messages.addObject()
                .put("MessageId", message.messageId)
                .put("ReceiptHandle", message.receiptHandle)
                .put("MD5OfBody", DigestUtils.md5Hex(message.body.getBytes(UTF_8)))
                .put("Body", message.body));
    }

    private synchronized void changeVisibility(String receiptHandle, Duration visibilityTimeout) {
        for (List<Message> queue : queues.values()) {
            for (Iterator<Message> messages = queue.iterator(); messages.hasNext(); ) {
                Message message = messages.next();
                if (receiptHandle.equals(message.receiptHandle)) {
                    if (visibilityTimeout == null) {
                        messages.remove();
                    } else {
                        message.visibleFrom = Instant.now().plus(visibilityTimeout);
                    }
                    notifyAll();
                }
            }
        }
    }

    private List<Message> queue(JsonNode request) {
        String queueUrl = request.path("QueueUrl").asText();
        return queues.computeIfAbsent(queueUrl.substring(queueUrl.lastIndexOf('/') + 1), name -> new ArrayList<>());
    }

    private static final class Message {
        private final String messageId;
        private final String body;
        private Instant visibleFrom;
        private String receiptHandle;

        private Message(String messageId, String body, Instant visibleFrom) {
            this.messageId = messageId;
            this.body = body;
            this.visibleFrom = visibleFrom;
        }
    }
}
